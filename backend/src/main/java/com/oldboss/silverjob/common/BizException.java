package com.oldboss.silverjob.common;

/**
 * 业务异常。
 * 用它表示“程序能继续运行，但当前请求不满足业务规则”的情况，例如未登录、参数不完整、状态不允许等。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    /**
     * 自定义错误码和错误信息，后续会被全局异常处理器转换成统一接口响应。
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
