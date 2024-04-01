package com.grace.train12306.framework.starter.convention.exception;


import com.grace.train12306.framework.starter.convention.errorcode.BaseErrorCode;
import com.grace.train12306.framework.starter.convention.errorcode.IErrorCode;
import lombok.ToString;

/**
 * 远程服务调用异常
 */
@ToString
public class RemoteException extends AbstractException {

    public RemoteException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public RemoteException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public RemoteException(String message, Throwable throwable, IErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
}
