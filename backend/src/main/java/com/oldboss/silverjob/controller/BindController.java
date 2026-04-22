package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.dto.BindConfirmRequestDTO;
import com.oldboss.silverjob.service.BindService;
import com.oldboss.silverjob.vo.BindInfoVO;
import com.oldboss.silverjob.vo.BindListItemVO;
import com.oldboss.silverjob.vo.BindOrderItemVO;
import com.oldboss.silverjob.vo.BindStatusVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 绑定控制器
 * 处理老人与子女的绑定关系，包括生成绑定码、确认绑定、解除绑定等接口
 */
@RestController
@RequestMapping("/api/bind")
public class BindController {

    private final BindService bindService;

    public BindController(BindService bindService) {
        this.bindService = bindService;
    }

    /**
     * 老人生成绑定码
     * @param authorization Authorization 请求头
     * @return 绑定码信息
     */
    @PostMapping("/createCode")
    public Result<BindStatusVO> createCode(@RequestHeader("Authorization") String authorization) {
        return Result.success(bindService.createBindCode(authorization), "已生成新绑定码");
    }

    /**
     * 子女确认绑定
     * @param authorization Authorization 请求头
     * @param request 绑定确认请求（包含绑定码）
     * @return 绑定成功结果
     */
    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestHeader("Authorization") String authorization,
                                                @Valid @RequestBody BindConfirmRequestDTO request) {
        bindService.confirmBind(authorization, request);
        return Result.success(null, "绑定成功");
    }

    /**
     * 解除绑定关系
     * @param authorization Authorization 请求头
     * @return 解除绑定结果
     */
    @PostMapping("/unbind")
    public Result<Void> unbind(@RequestHeader("Authorization") String authorization) {
        bindService.unbind(authorization);
        return Result.success(null, "已解除绑定");
    }

    /**
     * 获取绑定信息
     * @param authorization Authorization 请求头
     * @return 绑定信息、老人资料和进行中的订单
     */
    @GetMapping("/elderlyInfo")
    public Result<BindInfoVO> elderlyInfo(@RequestHeader("Authorization") String authorization) {
        return Result.success(bindService.bindInfo(authorization));
    }

    /**
     * 获取绑定老人相关的订单列表
     * @param authorization Authorization 请求头
     * @return 绑定老人的订单列表
     */
    @GetMapping("/orderList")
    public Result<?> orderList(@RequestHeader("Authorization") String authorization,
                               @RequestParam(value = "page", required = false) Integer page,
                               @RequestParam(value = "pageSize", required = false) Integer pageSize,
                               @RequestParam(value = "status", required = false) String status) {
        if (page == null && pageSize == null && status == null) {
            return Result.success(bindService.bindOrderList(authorization));
        }
        return Result.success(bindService.bindOrderList(authorization, page, pageSize, status));
    }

    /**
     * 获取所有绑定关系（管理员专用）
     * @param authorization Authorization 请求头
     * @return 所有绑定关系列表
     */
    @GetMapping("/list")
    public Result<?> list(@RequestHeader("Authorization") String authorization,
                          @RequestParam(value = "page", required = false) Integer page,
                          @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if (page == null && pageSize == null) {
            return Result.success(bindService.listAllBinds(authorization));
        }
        return Result.success(bindService.listAllBinds(authorization, page, pageSize));
    }
}
