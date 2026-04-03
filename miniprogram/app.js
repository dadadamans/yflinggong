const store = require("./utils/demo-store");

App({
  onLaunch() {
    store.initializeStore();
  },
  globalData: {
    appName: "银发零工"
  }
});
