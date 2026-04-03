const { authApi, homeApi, demoApi } = require("../../utils/api");

function getQuickEntries(role) {
  const entryMap = {
    elderly: [
      { title: "任务大厅", subtitle: "查看并接单", url: "/pages/elderly/tasks/index" },
      { title: "我的订单", subtitle: "查看服务进度", url: "/pages/elderly/orders/index" },
      { title: "个人中心", subtitle: "维护个人资料", url: "/pages/elderly/profile/index" },
      { title: "子女绑定", subtitle: "生成绑定码", url: "/pages/bind/index" }
    ],
    employer: [
      { title: "发布任务", subtitle: "新增服务需求", url: "/pages/employer/publish/index" },
      { title: "任务管理", subtitle: "查看已发布和接单状态", url: "/pages/employer/tasks/index" },
      { title: "订单管理", subtitle: "完成任务并查看结算", url: "/pages/employer/orders/index" },
      { title: "雇主资料", subtitle: "维护联系方式", url: "/pages/employer/profile/index" }
    ],
    child: [
      { title: "绑定老人", subtitle: "输入绑定码建立关系", url: "/pages/bind/index" },
      { title: "发布任务", subtitle: "替家里发布需求", url: "/pages/child/publish/index" },
      { title: "老人资料", subtitle: "查看健康和技能信息", url: "/pages/child/profile/index" },
      { title: "订单查看", subtitle: "跟进接单和完成状态", url: "/pages/child/orders/index" },
      { title: "留言沟通", subtitle: "给老人发送提醒", url: "/pages/child/messages/index" }
    ]
  };

  return entryMap[role] || entryMap.elderly;
}

function getRoleSummary(role) {
  const map = {
    elderly: {
      title: "老人工作台",
      desc: "这里只保留老人要用的功能：看任务、接单、查订单、维护资料和绑定子女，不放雇主发布和子女查看入口。"
    },
    employer: {
      title: "雇主工作台",
      desc: "这里只处理雇主职责：发布任务、查看任务是否被接、完成任务和维护雇主资料，不展示老人接单和子女留言功能。"
    },
    child: {
      title: "子女工作台",
      desc: "这里只处理子女视角：绑定老人、查看资料和订单、留言沟通，同时也可以替家里发布照护需求，但不做代接单。"
    }
  };

  return map[role] || map.elderly;
}

Page({
  data: {
    session: {},
    overview: {},
    quickEntries: []
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const [sessionRes, overviewRes] = await Promise.all([authApi.getSession(), homeApi.overview()]);

      if (!sessionRes.data.loggedIn) {
        wx.redirectTo({
          url: "/pages/login/index"
        });
        return;
      }

      this.setData({
        session: sessionRes.data,
        overview: Object.assign({}, overviewRes.data, {
          roleSummary: getRoleSummary(sessionRes.data.currentRole)
        }),
        quickEntries: getQuickEntries(sessionRes.data.currentRole)
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "页面加载失败",
        icon: "none"
      });
    }
  },
  goLogin() {
    wx.redirectTo({
      url: "/pages/login/index"
    });
  },
  async resetDemoData() {
    await demoApi.reset();
    wx.showToast({
      title: "已恢复初始数据",
      icon: "success"
    });
    this.goLogin();
  }
});
