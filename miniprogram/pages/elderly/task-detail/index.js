const { taskApi } = require("../../../utils/api");

Page({
  data: {
    taskId: "",
    task: null
  },
  onLoad(options) {
    this.setData({
      taskId: options.id || ""
    });
  },
  onShow() {
    this.loadDetail();
  },
  async loadDetail() {
    try {
      const res = await taskApi.detail(this.data.taskId);
      const task = Object.assign({}, res.data, {
        actionButtonClass: res.data.status === "waiting" ? "btn-accent" : "btn-primary",
        actionButtonText: res.data.status === "waiting" ? "确认接单" : "查看订单"
      });
      this.setData({
        task
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "任务加载失败",
        icon: "none"
      });
    }
  },
  async handleAction() {
    const task = this.data.task;

    if (!task) {
      return;
    }

    if (task.status === "waiting") {
      try {
        const res = await taskApi.grab(task.id);
        wx.showToast({
          title: res.message,
          icon: "success"
        });
        this.loadDetail();
      } catch (error) {
        wx.showToast({
          title: error.message || "接单失败",
          icon: "none"
        });
      }
      return;
    }

    wx.navigateTo({
      url: "/pages/elderly/orders/index"
    });
  }
});
