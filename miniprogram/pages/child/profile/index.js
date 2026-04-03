const { bindApi } = require("../../../utils/api");

Page({
  data: {
    bindStatusText: "待绑定",
    profile: {}
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const res = await bindApi.elderlyInfo();
      this.setData({
        bindStatusText: res.data.bindInfo.confirmed ? "已绑定" : "待绑定",
        profile: res.data.elderlyProfile
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  }
});
