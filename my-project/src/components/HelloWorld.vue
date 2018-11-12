<template>
  <div class="hello">
    测试
  </div>
</template>

<script>
export default {
  name: "HelloWorld",
  data() {
    return {};
  },
  mounted() {},
  methods: {
    async translate() {
      if (window.File && window.FileReader && window.FileList && window.Blob) {
        // Great success! All the File APIs are supported.
      } else {
        alert("The File APIs are not fully supported in this browser.");
      }s
    }
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
