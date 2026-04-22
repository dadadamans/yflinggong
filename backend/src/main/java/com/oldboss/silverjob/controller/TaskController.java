package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.common.TaskCategory;
import com.oldboss.silverjob.dto.IdRequestDTO;
import com.oldboss.silverjob.dto.TaskFormRequestDTO;
import com.oldboss.silverjob.service.TaskService;
import com.oldboss.silverjob.vo.CategoryVO;
import com.oldboss.silverjob.vo.OrderItemVO;
import com.oldboss.silverjob.vo.TaskDetailVO;
import com.oldboss.silverjob.vo.TaskItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 获取任务分类列表（用于级联选择器）
     * @return 任务分类数据，包含技能型、体验型、互助型三大类
     */
    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.success(TaskCategory.toCascadeList());
    }

    /**
     * 获取任务大厅列表
     * @param authorization Authorization 请求头
     * @return 当前用户可见的任务列表
     */
    @GetMapping("/list")
    public Result<?> list(@RequestHeader("Authorization") String authorization,
                          @RequestParam(value = "page", required = false) Integer page,
                          @RequestParam(value = "pageSize", required = false) Integer pageSize,
                          @RequestParam(value = "status", required = false) String status) {
        if (page == null && pageSize == null && status == null) {
            return Result.success(taskService.taskList(authorization));
        }
        return Result.success(taskService.taskList(authorization, page, pageSize, status));
    }

    /**
     * 获取任务详情
     * @param authorization Authorization 请求头
     * @param id 任务ID
     * @return 任务详情，若已被接单会同时带出订单信息
     */
    @GetMapping("/detail")
    public Result<TaskDetailVO> detail(@RequestHeader("Authorization") String authorization,
                                               @RequestParam("id") Long id) {
        return Result.success(taskService.taskDetail(authorization, id));
    }

    /**
     * 发布任务
     * @param authorization Authorization 请求头
     * @param request 任务表单数据（标题、类型、地址、薪资、时间、内容等）
     * @return 发布成功结果
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestHeader("Authorization") String authorization,
                                           @Valid @RequestBody TaskFormRequestDTO request) {
        taskService.addTask(authorization, request);
        return Result.success(null, "任务已发布");
    }

    /**
     * 老人用户申请接单
     * @param authorization Authorization 请求头
     * @param request 接单请求（包含任务ID）
     * @return 申请成功结果
     */
    @PostMapping("/apply")
    public Result<Void> apply(@RequestHeader("Authorization") String authorization,
                                           @RequestBody IdRequestDTO request) {
        taskService.applyTask(authorization, request.getId());
        return Result.success(null, "申请已提交，等待审核");
    }

    /**
     * 雇主批准接单
     * @param authorization Authorization 请求头
     * @param request 任务请求（包含任务ID）
     * @return 批准成功结果
     */
    @PostMapping("/approve")
    public Result<Void> approve(@RequestHeader("Authorization") String authorization,
                                              @RequestBody IdRequestDTO request) {
        taskService.approveTask(authorization, request.getId());
        return Result.success(null, "已批准接单");
    }

    /**
     * 雇主拒绝接单
     * @param authorization Authorization 请求头
     * @param request 任务请求（包含任务ID）
     * @return 拒绝成功结果
     */
    @PostMapping("/reject")
    public Result<Void> reject(@RequestHeader("Authorization") String authorization,
                                              @RequestBody IdRequestDTO request) {
        taskService.rejectTask(authorization, request.getId());
        return Result.success(null, "已拒绝申请");
    }

    /**
     * 管理员获取待接单任务列表
     * @param authorization Authorization 请求头
     * @return 待接单任务列表
     */
    @GetMapping("/admin/waiting")
    public Result<?> adminWaitingTasks(@RequestHeader("Authorization") String authorization,
                                       @RequestParam(value = "page", required = false) Integer page,
                                       @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            return Result.success(taskService.adminWaitingTasks(authorization));
        }
        return Result.success(taskService.adminWaitingTasks(authorization, page, pageSize));
    }

    /**
     * 获取订单列表
     * @param authorization Authorization 请求头
     * @return 当前用户的订单列表
     */
    @GetMapping("/orders")
    public Result<?> orders(@RequestHeader("Authorization") String authorization,
                            @RequestParam(value = "page", required = false) Integer page,
                            @RequestParam(value = "pageSize", required = false) Integer pageSize,
                            @RequestParam(value = "status", required = false) String status) {
        if (page == null && pageSize == null && status == null) {
            return Result.success(taskService.orderList(authorization));
        }
        return Result.success(taskService.orderList(authorization, page, pageSize, status));
    }

    /**
     * 老人完成订单
     * @param authorization Authorization 请求头
     * @param request 订单请求（包含任务ID）
     * @return 任务已完成结果
     */
    @PostMapping("/order/finish")
    public Result<Void> finishOrder(@RequestHeader("Authorization") String authorization,
                                                    @RequestBody IdRequestDTO request) {
        Long taskId = request.getId() != null ? request.getId() : request.getTaskId();
        taskService.finishOrder(authorization, taskId);
        return Result.success(null, "任务已完成");
    }

    /**
     * 雇主/子女支付订单
     * @param authorization Authorization 请求头
     * @param request 订单请求（包含任务ID）
     * @return 支付成功结果
     */
    @PostMapping("/order/pay")
    public Result<Void> payOrder(@RequestHeader("Authorization") String authorization,
                                                 @RequestBody IdRequestDTO request) {
        Long taskId = request.getId() != null ? request.getId() : request.getTaskId();
        taskService.payOrder(authorization, taskId);
        return Result.success(null, "支付成功");
    }

    /**
     * 取消订单
     * @param authorization Authorization 请求头
     * @param request 订单请求（包含任务ID）
     * @return 订单已取消结果
     */
    @PostMapping("/order/cancel")
    public Result<Void> cancelOrder(@RequestHeader("Authorization") String authorization,
                                                    @RequestBody IdRequestDTO request) {
        Long taskId = request.getId() != null ? request.getId() : request.getTaskId();
        taskService.cancelOrder(authorization, taskId);
        return Result.success(null, "订单已取消");
    }

    /**
     * 删除订单
     * @param authorization Authorization 请求头
     * @param request 订单请求（包含任务ID）
     * @return 订单已删除结果
     */
    @PostMapping("/order/delete")
    public Result<Void> deleteOrder(@RequestHeader("Authorization") String authorization,
                                                    @RequestBody IdRequestDTO request) {
        Long taskId = request.getId() != null ? request.getId() : request.getTaskId();
        taskService.deleteOrder(authorization, taskId);
        return Result.success(null, "订单已删除");
    }
}
