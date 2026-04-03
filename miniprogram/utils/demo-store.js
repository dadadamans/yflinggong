const STATUS_META = {
  waiting: {
    text: "待接单",
    className: "status-waiting"
  },
  working: {
    text: "进行中",
    className: "status-working"
  },
  done: {
    text: "已完成",
    className: "status-done"
  },
  cancelled: {
    text: "已取消",
    className: "status-cancelled"
  }
};

const ROLE_META = {
  elderly: {
    text: "老人",
    pageTitle: "老人工作台"
  },
  employer: {
    text: "雇主",
    pageTitle: "雇主工作台"
  },
  child: {
    text: "子女",
    pageTitle: "子女工作台"
  }
};

const STORAGE_KEY = "silver-job-full-demo-state";

const initialState = () => ({
  session: {
    loggedIn: false,
    currentRole: "elderly",
    token: ""
  },
  nextTaskId: 4,
  nextOrderId: 3,
  nextMessageId: 4,
  bindInfo: {
    code: "618204",
    confirmed: false,
    elderlyName: "王秀兰",
    childName: "王女士"
  },
  elderlyProfile: {
    nickname: "王阿姨",
    realName: "王秀兰",
    mobile: "13800001111",
    age: "63",
    gender: "女",
    city: "上海静安",
    healthDesc: "身体稳定，可完成半天以内的轻体力任务",
    skillTags: "陪诊、做饭、买菜",
    emergencyContact: "王女士",
    emergencyMobile: "13800002231"
  },
  employerProfile: {
    nickname: "李先生",
    name: "李先生",
    mobile: "13900006618",
    remark: "偏向发布社区附近的短时照护和陪诊任务"
  },
  childProfile: {
    nickname: "王女士",
    name: "王女士",
    relation: "女儿",
    mobile: "13700002231",
    note: "希望随时看到老人接单状态和留言"
  },
  tasks: [
    {
      id: 1,
      type: "陪诊",
      title: "社区医院陪同复查",
      address: "海棠社区卫生服务中心",
      timeText: "今天 14:00 - 17:00",
      salary: 120,
      content: "帮助挂号、取药和陪同问诊，路程近，流程简单。",
      publisherName: "李先生",
      publisherRole: "employer",
      status: "working",
      elderlyName: "王秀兰"
    },
    {
      id: 2,
      type: "家政",
      title: "午间做饭与厨房整理",
      address: "静安寺街道安福小区 3 栋 202",
      timeText: "明天 10:30 - 12:30",
      salary: 90,
      content: "准备两人午餐，饭后简单整理厨房。",
      publisherName: "李先生",
      publisherRole: "employer",
      status: "waiting",
      elderlyName: ""
    },
    {
      id: 3,
      type: "陪护",
      title: "上门陪伴老人聊天",
      address: "和睦家园 8 号楼",
      timeText: "后天 09:00 - 11:00",
      salary: 80,
      content: "陪老人散步、聊天，节奏轻松。",
      publisherName: "赵阿姨",
      publisherRole: "other",
      status: "done",
      elderlyName: "王秀兰"
    }
  ],
  orders: [
    {
      id: 1,
      taskId: 1,
      code: "#2026040301",
      title: "社区医院陪同复查",
      address: "海棠社区卫生服务中心",
      salary: 120,
      employerName: "李先生",
      elderlyName: "王秀兰",
      startTime: "2026-04-03 14:00",
      finishTime: "",
      status: "working",
      settleText: "待结算"
    },
    {
      id: 2,
      taskId: 3,
      code: "#2026040102",
      title: "上门陪伴老人聊天",
      address: "和睦家园 8 号楼",
      salary: 80,
      employerName: "赵阿姨",
      elderlyName: "王秀兰",
      startTime: "2026-04-01 09:00",
      finishTime: "2026-04-01 11:10",
      status: "done",
      settleText: "模拟已结算"
    }
  ],
  messages: [
    {
      id: 1,
      senderRole: "child",
      content: "妈，结束后记得休息一下，我晚上给您打电话。",
      time: "17:05"
    },
    {
      id: 2,
      senderRole: "elderly",
      content: "好的，今天任务不重，忙完我就回家。",
      time: "17:08"
    },
    {
      id: 3,
      senderRole: "child",
      content: "如果需要我帮忙联系雇主，随时告诉我。",
      time: "17:10"
    }
  ]
});

let state = initialState();

function canUseStorage() {
  return typeof wx !== "undefined" && typeof wx.getStorageSync === "function";
}

function saveState() {
  if (!canUseStorage()) {
    return;
  }

  wx.setStorageSync(STORAGE_KEY, state);
}

function initializeStore() {
  if (!canUseStorage()) {
    return;
  }

  const cached = wx.getStorageSync(STORAGE_KEY);
  if (cached && cached.tasks && cached.orders && cached.session) {
    state = cached;
    return;
  }

  saveState();
}

function clone(data) {
  return JSON.parse(JSON.stringify(data));
}

function getCurrentRole() {
  return state.session.currentRole || "elderly";
}

function getProfileByRole(role) {
  if (role === "elderly") {
    return state.elderlyProfile;
  }

  if (role === "employer") {
    return state.employerProfile;
  }

  return state.childProfile;
}

function getDisplayName(role) {
  const profile = getProfileByRole(role);

  if (role === "elderly") {
    return profile.realName || profile.nickname;
  }

  return profile.name || profile.nickname;
}

function syncDisplayName(role, oldName, newName) {
  if (!newName || oldName === newName) {
    return;
  }

  if (role === "elderly") {
    state.tasks.forEach((task) => {
      if (task.elderlyName === oldName) {
        task.elderlyName = newName;
      }
    });
    state.orders.forEach((order) => {
      if (order.elderlyName === oldName) {
        order.elderlyName = newName;
      }
    });
    state.bindInfo.elderlyName = newName;
  }

  if (role === "employer") {
    state.tasks.forEach((task) => {
      if (task.publisherName === oldName && task.publisherRole === "employer") {
        task.publisherName = newName;
      }
    });
    state.orders.forEach((order) => {
      if (order.employerName === oldName) {
        order.employerName = newName;
      }
    });
  }

  if (role === "child") {
    state.bindInfo.childName = newName;
  }
}

function buildTask(task) {
  const actionMap = {
    waiting: {
      actionText: "查看详情",
      actionClass: "btn-accent"
    },
    working: {
      actionText: "查看进度",
      actionClass: "btn-primary"
    },
    done: {
      actionText: "查看完成",
      actionClass: "btn-light"
    },
    cancelled: {
      actionText: "已取消",
      actionClass: "btn-light"
    }
  };

  return Object.assign({}, task, STATUS_META[task.status], actionMap[task.status], {
    elderlyLabel: task.elderlyName || "暂无",
    salaryText: `¥${task.salary}`
  });
}

function buildOrder(order) {
  return Object.assign({}, order, STATUS_META[order.status], {
    finishText: order.finishTime || "尚未完成"
  });
}

function buildMessage(message) {
  return Object.assign({}, message, {
    senderText: ROLE_META[message.senderRole].text
  });
}

function getTaskCounts(tasks) {
  return tasks.reduce(
    (result, task) => {
      result[task.status] += 1;
      return result;
    },
    {
      waiting: 0,
      working: 0,
      done: 0,
      cancelled: 0
    }
  );
}

function getSession() {
  return clone(state.session);
}

function login(payload) {
  const role = payload.role;

  if (!ROLE_META[role]) {
    return {
      ok: false,
      message: "请选择身份"
    };
  }

  const profile = getProfileByRole(role);
  const nickname = (payload.nickname || "").trim();
  const mobile = (payload.mobile || "").trim();

  if (nickname) {
    profile.nickname = nickname;
    if (role === "elderly") {
      profile.realName = nickname;
      state.bindInfo.elderlyName = nickname;
    } else if (role === "employer") {
      profile.name = nickname;
    } else {
      profile.name = nickname;
      state.bindInfo.childName = nickname;
    }
  }

  if (mobile) {
    profile.mobile = mobile;
  }

  state.session = {
    loggedIn: true,
    currentRole: role,
    token: `demo-${role}-${Date.now()}`
  };
  saveState();

  return {
    ok: true,
    data: getSession()
  };
}

function logout() {
  state.session.loggedIn = false;
  state.session.token = "";
  saveState();
  return {
    ok: true
  };
}

function getOverview() {
  const tasks = state.tasks.map(buildTask);
  const counts = getTaskCounts(state.tasks);
  const role = getCurrentRole();

  return {
    session: getSession(),
    currentRoleText: ROLE_META[role].text,
    currentProfileName: getDisplayName(role),
    stats: [
      {
        value: String(counts.waiting),
        label: "待接任务"
      },
      {
        value: String(counts.working),
        label: "进行中"
      },
      {
        value: String(state.orders.filter((item) => item.status === "done").length),
        label: "已完成订单"
      },
      {
        value: state.bindInfo.confirmed ? "已绑定" : "待绑定",
        label: "家属绑定"
      }
    ],
    latestTasks: tasks.slice(0, 2)
  };
}

function getUserInfo(role) {
  const targetRole = role || getCurrentRole();
  const profile = clone(getProfileByRole(targetRole));

  if (targetRole === "elderly") {
    profile.skillText = profile.skillTags;
  }

  return {
    role: targetRole,
    roleText: ROLE_META[targetRole].text,
    profile
  };
}

function saveUserInfo(role, payload) {
  const targetRole = role || getCurrentRole();
  const profile = getProfileByRole(targetRole);
  const oldName = getDisplayName(targetRole);

  Object.keys(payload).forEach((key) => {
    profile[key] = payload[key];
  });

  const newName = getDisplayName(targetRole);
  syncDisplayName(targetRole, oldName, newName);
  saveState();

  return {
    ok: true,
    message: "资料已保存"
  };
}

function getTaskList() {
  return state.tasks
    .slice()
    .sort((a, b) => a.id - b.id)
    .map(buildTask);
}

function getTaskDetail(taskId) {
  const task = state.tasks.find((item) => item.id === Number(taskId));

  if (!task) {
    return null;
  }

  const order = state.orders.find((item) => item.taskId === task.id);
  return Object.assign({}, buildTask(task), {
    order: order ? buildOrder(order) : null
  });
}

function getPublishedTaskList(role) {
  return state.tasks
    .filter((item) => item.publisherRole === role)
    .map(buildTask);
}

function addTask(form) {
  const title = (form.title || "").trim();
  const type = (form.type || "").trim();
  const address = (form.address || "").trim();
  const salary = Number(form.salary);
  const content = (form.content || "").trim();
  const timeText = (form.timeText || "").trim();

  if (!title || !type || !address || !salary || !content || !timeText) {
    return {
      ok: false,
      message: "请把任务信息填写完整"
    };
  }

  const currentRole = getCurrentRole();
  const publisherRole = currentRole === "child" ? "child" : "employer";
  const publisherName = getDisplayName(publisherRole);

  state.tasks.unshift({
    id: state.nextTaskId,
    type,
    title,
    address,
    timeText,
    salary,
    content,
    publisherName,
    publisherRole,
    status: "waiting",
    elderlyName: ""
  });
  state.nextTaskId += 1;
  saveState();

  return {
    ok: true,
    message: "任务已发布"
  };
}

function grabTask(taskId) {
  const task = state.tasks.find((item) => item.id === Number(taskId));

  if (!task) {
    return {
      ok: false,
      message: "任务不存在"
    };
  }

  if (task.status !== "waiting") {
    return {
      ok: false,
      message: "当前任务不能接单"
    };
  }

  task.status = "working";
  task.elderlyName = state.elderlyProfile.realName;

  state.orders.unshift({
    id: state.nextOrderId,
    taskId: task.id,
    code: `#20260403${String(state.nextOrderId).padStart(2, "0")}`,
    title: task.title,
    address: task.address,
    salary: task.salary,
    employerName: task.publisherName,
    elderlyName: state.elderlyProfile.realName,
    startTime: "2026-04-03 15:20",
    finishTime: "",
    status: "working",
    settleText: "待结算"
  });
  state.nextOrderId += 1;
  saveState();

  return {
    ok: true,
    message: "接单成功"
  };
}

function getOrderList(role) {
  const targetRole = role || getCurrentRole();
  let orders = state.orders.slice();

  if (targetRole === "employer") {
    orders = orders.filter((item) => item.employerName === state.employerProfile.name);
  }

  if (targetRole === "elderly" || targetRole === "child") {
    orders = orders.filter((item) => item.elderlyName === state.elderlyProfile.realName);
  }

  return orders.map(buildOrder);
}

function getPublishedOrderList(role) {
  const publishedTaskIds = state.tasks.filter((item) => item.publisherRole === role).map((item) => item.id);

  return state.orders
    .filter((item) => publishedTaskIds.includes(item.taskId))
    .map(buildOrder);
}

function finishOrder(taskId) {
  const task = state.tasks.find((item) => item.id === Number(taskId));
  const order = state.orders.find((item) => item.taskId === Number(taskId));

  if (!task || !order) {
    return {
      ok: false,
      message: "订单不存在"
    };
  }

  task.status = "done";
  order.status = "done";
  order.finishTime = "2026-04-03 17:30";
  order.settleText = "模拟已结算";
  saveState();

  return {
    ok: true,
    message: "任务已完成"
  };
}

function cancelOrder(taskId) {
  const task = state.tasks.find((item) => item.id === Number(taskId));
  const order = state.orders.find((item) => item.taskId === Number(taskId));

  if (!task || !order) {
    return {
      ok: false,
      message: "订单不存在"
    };
  }

  task.status = "cancelled";
  order.status = "cancelled";
  order.finishTime = "2026-04-03 16:10";
  order.settleText = "已取消，无需结算";
  saveState();

  return {
    ok: true,
    message: "订单已取消"
  };
}

function createBindCode() {
  state.bindInfo.code = String(Math.floor(100000 + Math.random() * 900000));
  state.bindInfo.confirmed = false;
  saveState();

  return clone(state.bindInfo);
}

function confirmBind(code) {
  if (!code) {
    return {
      ok: false,
      message: "请输入绑定码"
    };
  }

  if (String(code) !== state.bindInfo.code) {
    return {
      ok: false,
      message: "绑定码不正确"
    };
  }

  state.bindInfo.confirmed = true;
  saveState();

  return {
    ok: true,
    message: "绑定成功"
  };
}

function getBindInfo() {
  return clone(state.bindInfo);
}

function getChildElderlyInfo() {
  const workingOrder = state.orders.find((item) => item.status === "working");

  return {
    bindInfo: getBindInfo(),
    elderlyProfile: Object.assign({}, clone(state.elderlyProfile), {
      skillText: state.elderlyProfile.skillTags
    }),
    currentOrder: workingOrder ? buildOrder(workingOrder) : null
  };
}

function getMessageList() {
  return state.messages.map(buildMessage);
}

function addMessage(senderRole, content) {
  const text = (content || "").trim();

  if (!text) {
    return {
      ok: false,
      message: "请输入留言内容"
    };
  }

  state.messages.push({
    id: state.nextMessageId,
    senderRole,
    content: text,
    time: "18:20"
  });
  state.nextMessageId += 1;
  saveState();

  return {
    ok: true,
    message: "留言已发送"
  };
}

function resetDemo() {
  state = initialState();
  saveState();
}

module.exports = {
  initializeStore,
  getSession,
  login,
  logout,
  getOverview,
  getUserInfo,
  saveUserInfo,
  getTaskList,
  getTaskDetail,
  getPublishedTaskList,
  addTask,
  grabTask,
  getOrderList,
  getPublishedOrderList,
  finishOrder,
  cancelOrder,
  createBindCode,
  confirmBind,
  getBindInfo,
  getChildElderlyInfo,
  getMessageList,
  addMessage,
  resetDemo
};
