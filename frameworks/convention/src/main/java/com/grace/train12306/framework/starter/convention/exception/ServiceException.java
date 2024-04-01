package com.grace.train12306.framework.starter.convention.exception;


import com.grace.train12306.framework.starter.convention.errorcode.BaseErrorCode;
import com.grace.train12306.framework.starter.convention.errorcode.IErrorCode;
import lombok.ToString;

import java.util.Optional;

/**
 * 服务端异常
 */
@ToString
public class ServiceException extends AbstractException {

    public ServiceException(String message) {
        this(message, null, BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(IErrorCode errorCode) {
        this(null, errorCode);
    }

    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), throwable, errorCode);
    }
}
