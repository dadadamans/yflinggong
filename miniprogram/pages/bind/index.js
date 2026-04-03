const { bindApi } = require("../../utils/api");

Page({
  data: {
    bindCode: "",
    inputCode: "",
    bindStatusText: "待确认"
  },
  onShow() {
    this.loadInfo();
  },
  async loadInfo() {
    const res = await bindApi.info();
    this.setData({
      bindCode: res.data.code,
      bindStatusText: res.data.confirmed ? "已绑定" : "待确认"
    });
  },
  onInput(event) {
    this.setData({
      inputCode: event.detail.value
    });
  },
  async refreshCode() {
    const res = await bindApi.createCode();
    this.setData({
      bindCode: res.data.code,
      inputCode: "",
      bindStatusText: "待确认"
    });
    wx.showToast({
      title: "已生成新绑定码",
      icon: "success"
    });
  },
  async confirmBind() {
    try {
      const res = await bindApi.confirm(this.data.inputCode);
      wx.showToast({
        title: res.message,
        icon: "success"
      });
      this.loadInfo();
    } catch (error) {
      wx.showToast({
        title: error.message || "绑定失败",
        icon: "none"
      });
    }
  }
});
