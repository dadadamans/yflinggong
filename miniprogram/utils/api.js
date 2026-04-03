const store = require("./demo-store");

function delay(result, shouldReject) {
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (shouldReject) {
        reject(result);
        return;
      }

      resolve(result);
    }, 120);
  });
}

function ok(data, message) {
  return delay({
    code: 200,
    data,
    message: message || "ok"
  });
}

function fail(message) {
  return delay(
    {
      code: 400,
      message
    },
    true
  );
}

function handleResult(result) {
  if (!result.ok) {
    return fail(result.message);
  }

  return ok(result.data || {}, result.message);
}

const authApi = {
  wxLogin(payload) {
    return handleResult(store.login(payload));
  },
  logout() {
    return handleResult(store.logout());
  },
  getSession() {
    return ok(store.getSession());
  }
};

const homeApi = {
  overview() {
    return ok(store.getOverview());
  }
};

const userApi = {
  getInfo(role) {
    return ok(store.getUserInfo(role));
  },
  save(payload, role) {
    return handleResult(store.saveUserInfo(role, payload));
  }
};

const taskApi = {
  list() {
    return ok(store.getTaskList());
  },
  published(role) {
    return ok(store.getPublishedTaskList(role));
  },
  detail(id) {
    const task = store.getTaskDetail(id);
    if (!task) {
      return fail("任务不存在");
    }

    return ok(task);
  },
  add(payload) {
    return handleResult(store.addTask(payload));
  },
  grab(id) {
    return handleResult(store.grabTask(id));
  }
};

const orderApi = {
  list(role) {
    return ok(store.getOrderList(role));
  },
  published(role) {
    return ok(store.getPublishedOrderList(role));
  },
  finish(taskId) {
    return handleResult(store.finishOrder(taskId));
  },
  cancel(taskId) {
    return handleResult(store.cancelOrder(taskId));
  }
};

const bindApi = {
  createCode() {
    return ok(store.createBindCode());
  },
  confirm(code) {
    return handleResult(store.confirmBind(code));
  },
  info() {
    return ok(store.getBindInfo());
  },
  elderlyInfo() {
    return ok(store.getChildElderlyInfo());
  },
  orderList() {
    return ok(store.getOrderList("child"));
  }
};

const messageApi = {
  list() {
    return ok(store.getMessageList());
  },
  send(senderRole, content) {
    return handleResult(store.addMessage(senderRole, content));
  }
};

const demoApi = {
  reset() {
    store.resetDemo();
    return ok({}, "已恢复初始演示数据");
  }
};

module.exports = {
  authApi,
  homeApi,
  userApi,
  taskApi,
  orderApi,
  bindApi,
  messageApi,
  demoApi
};
