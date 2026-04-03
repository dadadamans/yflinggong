const { userApi } = require("../../../utils/api");

Page({
  data: {
    form: {
      name: "",
      mobile: "",
      remark: ""
    }
  },
  onShow() {
    this.loadInfo();
  },
  async loadInfo() {
    const res = await userApi.getInfo("employer");
    this.setData({
      form: res.data.profile
    });
  },
  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({
      [`form.${field}`]: event.detail.value
    });
  },
  async saveProfile() {
    try {
      const res = await userApi.save(this.data.form, "employer");
      wx.showToast({
        title: res.message,
        icon: "success"
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "保存失败",
        icon: "none"
      });
    }
  }
});
