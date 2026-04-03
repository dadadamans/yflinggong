const { userApi } = require("../../../utils/api");

Page({
  data: {
    form: {
      realName: "",
      mobile: "",
      age: "",
      gender: "",
      city: "",
      healthDesc: "",
      skillTags: "",
      emergencyContact: "",
      emergencyMobile: ""
    }
  },
  onShow() {
    this.loadInfo();
  },
  async loadInfo() {
    const res = await userApi.getInfo("elderly");
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
      const res = await userApi.save(this.data.form, "elderly");
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
