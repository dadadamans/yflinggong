const { messageApi } = require("../../../utils/api");

Page({
  data: {
    messages: [],
    messageInput: ""
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const res = await messageApi.list();
      this.setData({
        messages: res.data.map((item) =>
          Object.assign({}, item, {
            bubbleClass: item.senderRole === "child" ? "bubble-self" : "bubble-other"
          })
        )
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  },
  onMessageInput(event) {
    this.setData({
      messageInput: event.detail.value
    });
  },
  async sendMessage() {
    try {
      const res = await messageApi.send("child", this.data.messageInput);
      wx.showToast({
        title: res.message,
        icon: "success"
      });
      this.setData({
        messageInput: ""
      });
      this.loadPage();
    } catch (error) {
      wx.showToast({
        title: error.message || "发送失败",
        icon: "none"
      });
    }
  }
});
