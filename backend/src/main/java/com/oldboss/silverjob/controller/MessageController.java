package com.oldboss.silverjob.controller;

import com.oldboss.silverjob.common.Result;
import com.oldboss.silverjob.dto.MessageSendRequestDTO;
import com.oldboss.silverjob.service.MessageService;
import com.oldboss.silverjob.vo.MessageItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 留言控制器
 * 处理老人与子女之间的留言沟通接口
 */
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 获取留言列表
     * @param authorization Authorization 请求头
     * @return 绑定关系下的留言列表
     */
    @GetMapping("/list")
    public Result<List<MessageItemVO>> list(@RequestHeader("Authorization") String authorization) {
        return Result.success(messageService.messageList(authorization));
    }

    /**
     * 发送留言
     * @param authorization Authorization 请求头
     * @param request 留言请求（包含留言内容）
     * @return 发送成功结果
     */
    @PostMapping("/send")
    public Result<Void> send(@RequestHeader("Authorization") String authorization,
                                             @Valid @RequestBody MessageSendRequestDTO request) {
        messageService.sendMessage(authorization, request);
        return Result.success(null, "留言已发送");
    }
}
