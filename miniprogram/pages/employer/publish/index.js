const { taskApi, orderApi } = require("../../../utils/api");

const defaultForm = {
  title: "陪老人去医院复查",
  type: "陪诊服务",
  address: "静安区海防路 188 号",
  salary: "150",
  timeText: "2026-04-04 09:00 - 12:00",
  content: "需要一位耐心细致的阿姨陪同就诊，帮助排队和取药。"
};

Page({
  data: {
    form: Object.assign({}, defaultForm)
  },
  onShow() {
    this.setData({
      form: Object.assign({}, defaultForm)
    });
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
      wx.navigateTo({
        url: "/pages/employer/tasks/index"
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "发布失败",
        icon: "none"
      });
    }
  }
});
