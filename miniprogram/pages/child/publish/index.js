const { taskApi, orderApi } = require("../../../utils/api");

const defaultForm = {
  title: "替家里预约医院陪诊",
  type: "陪诊服务",
  address: "静安区社区医院",
  salary: "160",
  timeText: "2026-04-05 09:00 - 12:00",
  content: "希望找一位细心的阿姨陪老人看诊、排队和取药。"
};

Page({
  data: {
    form: Object.assign({}, defaultForm),
    tasks: [],
    orders: []
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const [taskRes, orderRes] = await Promise.all([taskApi.published("child"), orderApi.published("child")]);
      this.setData({
        tasks: taskRes.data,
        orders: orderRes.data
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  },
  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({
      [`form.${field}`]: event.detail.value
    });
  },
  async submitTask() {
    try {
      const res = await taskApi.add(this.data.form);
      wx.showToast({
        title: res.message,
        icon: "success"
      });
      this.setData({
        form: Object.assign({}, defaultForm)
      });
      this.loadPage();
    } catch (error) {
      wx.showToast({
        title: error.message || "发布失败",
        icon: "none"
      });
    }
  }
});
