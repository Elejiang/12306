package com.grace.train12306.framework.starter.convention.exception;

import com.grace.train12306.framework.starter.convention.errorcode.BaseErrorCode;
import com.grace.train12306.framework.starter.convention.errorcode.IErrorCode;
import lombok.ToString;

/**
 * 客户端异常
 */
@ToString
public class ClientException extends AbstractException {

    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
}
