const { orderApi, bindApi, messageApi } = require("../../../utils/api");

Page({
  data: {
    orders: [],
    bindCode: "",
    messages: [],
    replyInput: ""
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const [orderRes, bindRes, messageRes] = await Promise.all([
        orderApi.list("elderly"),
        bindApi.info(),
        messageApi.list()
      ]);

      this.setData({
        orders: orderRes.data,
        bindCode: bindRes.data.code,
        messages: messageRes.data.map((item) =>
          Object.assign({}, item, {
            bubbleClass: item.senderRole === "elderly" ? "bubble-self" : "bubble-other"
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
  onReplyInput(event) {
    this.setData({
      replyInput: event.detail.value
    });
  },
  async sendReply() {
    try {
      const res = await messageApi.send("elderly", this.data.replyInput);
      wx.showToast({
        title: res.message,
        icon: "success"
      });
      this.setData({
        replyInput: ""
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
