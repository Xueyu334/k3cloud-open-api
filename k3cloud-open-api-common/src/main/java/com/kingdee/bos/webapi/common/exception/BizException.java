package com.kingdee.bos.webapi.common.exception;

import java.io.Serial;

/**
 * WebAPI 模块内部的业务异常基类。
 */
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -3523268774413159621L;

    protected final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BizException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public BizException(Throwable cause) {
        super(cause);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "BizException{" +
                "code=" + code +
                '}';
    }
}
