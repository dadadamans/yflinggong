const { bindApi } = require("../../../utils/api");

Page({
  data: {
    orders: [],
    currentOrder: null
  },
  onShow() {
    this.loadPage();
  },
  async loadPage() {
    try {
      const [elderlyRes, orderRes] = await Promise.all([bindApi.elderlyInfo(), bindApi.orderList()]);
      this.setData({
        currentOrder: elderlyRes.data.currentOrder,
        orders: orderRes.data
      });
    } catch (error) {
      wx.showToast({
        title: error.message || "加载失败",
        icon: "none"
      });
    }
  }
});
