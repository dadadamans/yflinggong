const { taskApi } = require("../../../utils/api");

Page({
  data: {
    tasks: []
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const res = await taskApi.list();
      const tasks = res.data
        .filter((item) => item.publisherRole === "employer")
        .map((item) =>
          Object.assign({}, item, {
            hintText:
              item.status === "waiting"
                ? "任务已发布，等待老人端接单"
                : item.status === "working"
                  ? "老人已接单，去订单管理页完成任务"
                  : item.status === "done"
                    ? "任务已完成，可在订单页查看结算"
                    : "当前任务已取消"
          })
        );

      this.setData({
        tasks
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  }
});
