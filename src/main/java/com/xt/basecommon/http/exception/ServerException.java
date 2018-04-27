package com.xt.basecommon.http.exception;

/**
 * Describe: 服务器异常
 * Created by lijin on 2017/9/25.
 */

public class ServerException extends RuntimeException {

    private int errCode;
    private String message;

    public ServerException(int errCode, String msg) {
        super(msg);
        this.errCode = errCode;
        this.message = msg;
    }

    public int getErrCode() {
        return errCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
