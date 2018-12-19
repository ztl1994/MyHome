package com.open.test2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.open.test2.adapter.BlueToothDeviceAdapter;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.open.buletoothBle.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.open.buletoothBle.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.open.buletoothBle.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.open.buletoothBle.ACTION_DATA_AVAILABLE";
    public final static String READ_RSSI = "com.open.buletoothBle.READ_RSSI";
    public final static String EXTRA_DATA = "com.open.buletoothBle.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public List<BluetoothGattCharacteristic> writeUuid = new ArrayList<>();
    public List<BluetoothGattCharacteristic> readUuid = new ArrayList<>();
    public List<BluetoothGattCharacteristic> notifyUuid = new ArrayList<>();

    private static final String TAG = "BleManager";
    private BluetoothAdapter bTAdatper;
    private ListView listView;
    private ListView bondlistView;
    private BluetoothManager bluetoothManager;

    private BlueToothDeviceAdapter adapter;
    private BlueToothDeviceAdapter bondAdapter;

    private TextView text_state;
    private TextView text_msg;

//    private final int BUFFER_SIZE = 1024;
//    private static final String NAME = "BT_DEMO";
//    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
//    private ConnectThread connectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        initView();

        bTAdatper = bluetoothManager.getAdapter();
        initReceiver();
    }

    private void initView() {
        findViewById(R.id.btn_openBT).setOnClickListener(this);
        findViewById(R.id.btn_search).setOnClickListener(this);
        findViewById(R.id.btn_send).setOnClickListener(this);
        text_state = (TextView) findViewById(R.id.text_state);
        text_msg = (TextView) findViewById(R.id.text_msg);

        listView = (ListView) findViewById(R.id.listView);
        bondlistView = (ListView) findViewById(R.id.bondListView);
        adapter = new BlueToothDeviceAdapter(getApplicationContext(), R.layout.bluetooth_device_list_item);
        bondAdapter = new BlueToothDeviceAdapter(getApplicationContext(), R.layout.bluetooth_device_list_item);
        listView.setAdapter(adapter);
        bondlistView.setAdapter(bondAdapter);

        bondlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bTAdatper.isDiscovering()) {
                    bTAdatper.cancelDiscovery();
                }
                BluetoothDevice device = (BluetoothDevice) bondAdapter.getItem(position);
//                connectDevice(device);
                connect(device.getAddress());
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bTAdatper.isDiscovering()) {
                    bTAdatper.cancelDiscovery();
                }
                BluetoothDevice device = (BluetoothDevice) adapter.getItem(position);
                // 配对
                pin(device);
//                connectDevice(device);
            }
        });
    }

    private void initReceiver() {
        //注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        filter.addAction(ACTION_GATT_CONNECTED);
        filter.addAction(ACTION_GATT_DISCONNECTED);
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(ACTION_DATA_AVAILABLE);
        filter.addAction(READ_RSSI);

        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_openBT:
                openBlueTooth();
                break;
            case R.id.btn_search:
                searchDevices();
                break;
            case R.id.btn_send:
//                if (connectThread != null) {
//                    connectThread.sendMsg("这是蓝牙发送过来的消息");
//                }
                writeDate(true);
                break;
        }
    }


    /**
     * 开启蓝牙
     */
    private void openBlueTooth() {
        final int REQUEST_ENABLE_BT = 2;
        if (bTAdatper == null) {
            Toast.makeText(this, "当前设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
        }
        if (!bTAdatper.isEnabled()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, REQUEST_ENABLE_BT);
            startActivity(i);
            bTAdatper.enable();
        }
        //开启被其它蓝牙设备发现的功能
        if (bTAdatper.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //设置为一直开启
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(i);
        }
    }

    /**
     * 搜索蓝牙设备
     */
    private void searchDevices() {
        if (bTAdatper.isDiscovering()) {
            bTAdatper.cancelDiscovery();
        }
        getBoundedDevices();
        bTAdatper.startDiscovery();
    }

    /**
     * 获取已经配对过的设备
     */
    private void getBoundedDevices() {
        //获取已经配对过的设备
        Set<BluetoothDevice> pairedDevices = bTAdatper.getBondedDevices();
        bondAdapter.clear();
        //将其添加到设备列表中
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                bondAdapter.add(device);
            }
        }
    }

    /**
     * 配对（配对成功与失败通过广播返回）
     *
     * @param device
     */
    public void pin(BluetoothDevice device) {
        if (device == null) {
            return;
        }
        //判断设备是否配对，没有配对在配，配对了就不需要配了
        int a = device.getBondState();
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            try {
                Method createBondMethod = device.getClass().getMethod("createBond");
                Boolean returnValue = (Boolean) createBondMethod.invoke(device);
                returnValue.booleanValue();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 取消配对（取消配对成功与失败通过广播返回 也就是配对失败）
     *
     * @param device
     */
    public void cancelPinBule(BluetoothDevice device) {
        if (device == null) {
            return;
        }
        //判断设备是否配对，没有配对就不用取消了
        if (device.getBondState() != BluetoothDevice.BOND_NONE) {
            try {
                Method removeBondMethod = device.getClass().getMethod("removeBond");
                Boolean returnValue = (Boolean) removeBondMethod.invoke(device);
                returnValue.booleanValue();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public boolean connect(final String address) {//4

        if (bTAdatper == null || address == null) {
            Log.d(TAG, "BluetoothAdapter不能初始化 or 未知 address.");
            return false;
        }

        // 以前连接过的设备，重新连接
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "尝试使用现在的 mBluetoothGatt连接.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bTAdatper.getRemoteDevice(address);
        if (device == null) {
            Log.d(TAG, "设备没找到，不能连接");
            return false;
        }
        text_state.setText(getResources().getString(R.string.connecting));
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);//真正的连接
        //这个方法需要三个参数：一个Context对象，自动连接（boolean值,表示只要BLE设备可用是否自动连接到它），和BluetoothGattCallback调用。
        Log.d(TAG, "尝试新的连接.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            Log.d(TAG, "status" + status);
            if (newState == BluetoothProfile.STATE_CONNECTED) {//当连接状态发生改变
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);//发送广播
                Log.d(TAG, "连接GATT server");
                // 连接成功后尝试发现服务
                //通过mBluetoothGatt.discoverServices()，我们就可以获取到ble设备的所有Services。
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {//当设备无法连接
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "断开连接");
                broadcastUpdate(intentAction);   //发送广播
            }
        }

        @Override
        // 发现新服务，即调用了mBluetoothGatt.discoverServices()后，返回的数据
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //得到所有Service
                List<BluetoothGattService> supportedGattServices = gatt.getServices();

                for (BluetoothGattService gattService : supportedGattServices) {
                    //得到每个Service的Characteristics
                    List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        int charaProp = gattCharacteristic.getProperties();
                        //所有Characteristics按属性分类
                        if (charaProp == 2) {
                            Log.d(TAG, "gattCharacteristic的UUID为:" + gattCharacteristic.getUuid());
                            Log.d(TAG, "gattCharacteristic的属性为:  可读");
                            readUuid.add(gattCharacteristic);
                        }
                        if ((charaProp == 8)) {
                            Log.d(TAG, "gattCharacteristic的UUID为:" + gattCharacteristic.getUuid());
                            Log.d(TAG, "gattCharacteristic的属性为:  可写");
                            writeUuid.add(gattCharacteristic);
                        }
                        if ((charaProp == 16)) {
                            Log.d(TAG, "gattCharacteristic的UUID为:" + gattCharacteristic.getUuid() + gattCharacteristic);
                            Log.d(TAG, "gattCharacteristic的属性为:  具备通知属性");
                            notifyUuid.add(gattCharacteristic);
                        }
                    }
                }

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // 读写特性
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {

        }

        //如果对一个特性启用通知,当远程蓝牙设备特性发送变化，回调函数onCharacteristicChanged( ))被触发。
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            //mBluetoothGatt.readRemoteRssi()调用得到，rssi即信号强度，做防丢器时可以不断使用此方法得到最新的信号强度，从而得到距离。
            broadcastUpdate(READ_RSSI);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {

            System.out.println("--------write success----- status:" + status);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" +
                        stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消搜索
        if (bTAdatper != null && bTAdatper.isDiscovering()) {
            bTAdatper.cancelDiscovery();
        }
        //注销BroadcastReceiver，防止资源泄露
        unregisterReceiver(mReceiver);
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //避免重复添加已经绑定过的设备
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        adapter.add(device);
                        adapter.notifyDataSetChanged();
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    Toast.makeText(MainActivity.this, "开始搜索", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Toast.makeText(MainActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_GATT_CONNECTED:
                    text_state.post(new Runnable() {
                        @Override
                        public void run() {
                            text_state.setText(getResources().getString(R.string.connect_success));
                        }
                    });
                    break;
                case ACTION_GATT_DISCONNECTED:
                    text_state.post(new Runnable() {
                        @Override
                        public void run() {
                            text_state.setText(getResources().getString(R.string.connect_error));
                        }
                    });
                    break;

            }
        }
    };


    private void writeDate(final boolean connect) {
        BluetoothGattCharacteristic characteristic;
        final String msg = "34234";
        if (connect && writeUuid.size() > 0) {

            characteristic = writeUuid.get(0);
            write(characteristic, msg);
            writeCharacteristic(characteristic);
            text_msg.post(new Runnable() {
                @Override
                public void run() {
                    text_msg.setText(getResources().getString(R.string.send_msgs) + msg);
                }
            });
        } else {
            Log.d("123", "发送数据失败");
        }
    }

    private void write(BluetoothGattCharacteristic characteristic, byte byteArray[]) {
        characteristic.setValue(byteArray);
    }

    private void write(BluetoothGattCharacteristic characteristic, String string) {
        characteristic.setValue(string);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (bTAdatper == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        } else mBluetoothGatt.writeCharacteristic(characteristic);

    }

    /**
     * 以下为sockt 连接方式，尚未完善
     */
    /**
     * 连接蓝牙设备
     */
//    private void connectDevice(BluetoothDevice device) {
//        text_state.setText(getResources().getString(R.string.connecting));
//        try {
//            //启动连接线程
//            connectThread = new ConnectThread(device, true);
//            connectThread.start();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 连接线程
//     */
//    private class ConnectThread extends Thread {
//
//        private BluetoothSocket socket;
//        private boolean activeConnect;
//        InputStream inputStream;
//        OutputStream outputStream;
//        BluetoothDevice mmDevice;
//
//        private ConnectThread(BluetoothDevice mmDevice, boolean connect) {
//            this.mmDevice = mmDevice;
//            this.activeConnect = connect;
//
//            try {
////                this.socket = mmDevice.createInsecureRfcommSocketToServiceRecord(BT_UUID);
//
//                socket = (BluetoothSocket) mmDevice.getClass()
//                        .getDeclaredMethod("createRfcommSocket", new Class[]{int.class})
//                        .invoke(mmDevice, 1);
//                Thread.sleep(500);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
//
//        @Override
//        public void run() {
//            bTAdatper.cancelDiscovery();
//
//            if (activeConnect && socket != null) {
//                //如果是自动连接 则调用连接方法
//                try {
//                    socket.connect();
//                    text_state.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            text_state.setText(getResources().getString(R.string.connect_success));
//                        }
//                    });
//
//
//                } catch (IOException e) {
//                    text_state.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            text_state.setText(getResources().getString(R.string.connect_error));
//                        }
//                    });
//                    e.printStackTrace();
//                }
//            }
//
//            try {
//                inputStream = socket.getInputStream();
//                outputStream = socket.getOutputStream();
//
//
//                byte[] buffer = new byte[BUFFER_SIZE];
//                int bytes;
//                while (true) {
//                    //读取数据
//                    bytes = inputStream.read(buffer);
//                    if (bytes > 0) {
//                        final byte[] data = new byte[bytes];
//                        System.arraycopy(buffer, 0, data, 0, bytes);
//                        text_msg.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                text_msg.setText(getResources().getString(R.string.get_msg) + new String(data));
//                            }
//                        });
//                    }
//                }
//            } catch (IOException e2) {
//
//            }
//        }
//
//        /**
//         * 发送数据
//         *
//         * @param msg
//         */
//        public void sendMsg(final String msg) {
//
//            byte[] bytes = msg.getBytes();
//            if (outputStream != null) {
//                try {
//                    //发送数据
//                    outputStream.write(bytes);
//                    text_msg.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            text_msg.setText(getResources().getString(R.string.send_msgs) + msg);
//                        }
//                    });
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    text_msg.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            text_msg.setText(getResources().getString(R.string.send_msg_error) + msg);
//                        }
//                    });
//                }
//            }
//        }
//    }
//
//    /**
//     * 监听线程
//     */
//    private class ListenerThread extends Thread {
//        private final BluetoothServerSocket serverSocket;
//
//        public ListenerThread() {
//            BluetoothServerSocket tmp = null;
//            try {
//                tmp = bTAdatper.listenUsingRfcommWithServiceRecord(NAME, BT_UUID);
//            } catch (IOException e) {
//            }
//            serverSocket = tmp;
//        }
//
//        @Override
//        public void run() {
//            BluetoothSocket socket = null;
//            while (true) {
//                try {
//
//                    socket = serverSocket.accept();
//                    if (socket != null) {
//                        connectThread = new ConnectThread(socket.getRemoteDevice(), false);
//                        connectThread.start();
//                        serverSocket.close();
//                        break;
//                    }
//                } catch (IOException e) {
//                    break;
//                }
//                text_state.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        text_state.setText(getResources().getString(R.string.connecting));
//                    }
//                });
//            }
//        }
//    }
}
