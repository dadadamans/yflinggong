const { taskApi, userApi } = require("../../../utils/api");

Page({
  data: {
    profile: {},
    tasks: [],
    waitingCount: 0,
    activeCount: 0
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const [userRes, taskRes] = await Promise.all([userApi.getInfo("elderly"), taskApi.list()]);
      const tasks = taskRes.data;

      this.setData({
        profile: userRes.data.profile,
        tasks,
        waitingCount: tasks.filter((item) => item.status === "waiting").length,
        activeCount: tasks.filter((item) => item.status === "working").length
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  },
  goDetail(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({
      url: `/pages/elderly/task-detail/index?id=${id}`
    });
  }
});
