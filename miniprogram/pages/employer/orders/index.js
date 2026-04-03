const { orderApi } = require("../../../utils/api");

Page({
  data: {
    orders: []
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const res = await orderApi.list("employer");
      const orders = res.data.map((item) =>
        Object.assign({}, item, {
          actionText: item.status === "working" ? "完成任务" : item.status === "done" ? "已完成" : "查看状态",
          actionClass: item.status === "working" ? "btn-accent" : "btn-light"
        })
      );
      this.setData({
        orders
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  },
  async finishOrder(event) {
    const { taskId, status } = event.currentTarget.dataset;

    if (status !== "working") {
      wx.showToast({
        title: "当前订单无需完成",
        icon: "none"
      });
      return;
    }

    try {
      const res = await orderApi.finish(taskId);
      wx.showToast({
        title: res.message,
        icon: "success"
      });
      this.loadPage();
    } catch (error) {
      wx.showToast({
        title: error.message || "操作失败",
        icon: "none"
      });
    }
  }
});
