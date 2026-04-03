const { bindApi, messageApi, orderApi } = require("../../utils/api");

Page({
  data: {
    bindStatusText: "待绑定",
    profileName: "",
    profileNameText: "未绑定",
    activeOrderCount: 0,
    latestMessage: "暂无留言"
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const [elderlyRes, orderRes, messageRes] = await Promise.all([
        bindApi.elderlyInfo(),
        orderApi.list("child"),
        messageApi.list()
      ]);

      const messages = messageRes.data;

      this.setData({
        bindStatusText: elderlyRes.data.bindInfo.confirmed ? "已绑定" : "待绑定",
        profileName: elderlyRes.data.elderlyProfile.realName,
        profileNameText: elderlyRes.data.elderlyProfile.realName || "未绑定",
        activeOrderCount: orderRes.data.filter((item) => item.status === "working").length,
        latestMessage: messages.length ? messages[messages.length - 1].content : "暂无留言"
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  }
});
