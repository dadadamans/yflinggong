const { authApi } = require("../../utils/api");

const roleOptions = [
  {
    value: "elderly",
    title: "老人用户",
    subtitle: "浏览任务、接单、查看订单"
  },
  {
    value: "employer",
    title: "雇主用户",
    subtitle: "发布任务、管理订单"
  },
  {
    value: "child",
    title: "子女用户",
    subtitle: "绑定老人、查看状态、留言"
  }
];

const presetMap = {
  elderly: {
    nickname: "王秀兰",
    mobile: "13800001111"
  },
  employer: {
    nickname: "李先生",
    mobile: "13900006618"
  },
  child: {
    nickname: "王女士",
    mobile: "13700002231"
  }
};

Page({
  data: {
    roleOptions: roleOptions.map((item, index) =>
      Object.assign({}, item, {
        active: index === 0,
        activeClass: index === 0 ? "option-card-active" : ""
      })
    ),
    selectedRole: "elderly",
    form: Object.assign({}, presetMap.elderly)
  },
  selectRole(event) {
    const role = event.currentTarget.dataset.role;
    this.setData({
      selectedRole: role,
      form: Object.assign({}, presetMap[role]),
      roleOptions: roleOptions.map((item) =>
        Object.assign({}, item, {
          active: item.value === role,
          activeClass: item.value === role ? "option-card-active" : ""
        })
      )
    });
  },
  onInput(event) {
    const field = event.currentTarget.dataset.field;
    this.setData({
      [`form.${field}`]: event.detail.value
    });
  },
  async submitLogin() {
    try {
      await authApi.wxLogin({
        role: this.data.selectedRole,
        nickname: this.data.form.nickname,
        mobile: this.data.form.mobile
      });

      wx.redirectTo({
        url: "/pages/home/index"
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "登录失败",
        icon: "none"
      });
    }
  }
});
