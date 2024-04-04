package com.grace.train12306.biz.userservice.common.enums;

import com.grace.train12306.framework.starter.convention.errorcode.IErrorCode;
import lombok.AllArgsConstructor;

/**
 * 用户注册错误码枚举
 */
@AllArgsConstructor
public enum UserRegisterErrorCodeEnum implements IErrorCode {

    USER_REGISTER_FAIL("A006000", "用户注册失败"),

    USER_NAME_NOTNULL("A006001", "用户名不能为空"),

    PASSWORD_NOTNULL("A006002", "密码不能为空"),

    PHONE_NOTNULL("A006003", "手机号不能为空"),

    ID_TYPE_NOTNULL("A006004", "证件类型不能为空"),

    ID_CARD_NOTNULL("A006005", "证件号不能为空"),

    HAS_USERNAME_NOTNULL("A006006", "用户名已存在"),

    PHONE_REGISTERED("A006007", "手机号已被占用"),

    MAIL_REGISTERED("A006008", "邮箱已被占用"),

    MAIL_NOTNULL("A006009", "邮箱不能为空"),

    USER_TYPE_NOTNULL("A006010", "旅客类型不能为空"),

    POST_CODE_NOTNULL("A006011", "邮编不能为空"),

    ADDRESS_NOTNULL("A006012", "地址不能为空"),

    REGION_NOTNULL("A006012", "国家/地区不能为空"),

    TELEPHONE_NOTNULL("A006013", "固定电话不能为空"),

    VERIFY_STATE_NOTNULL("A006014", "审核状态不能为空"),

    REAL_NAME_NOTNULL("A006015", "真实姓名不能为空");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误提示消息
     */
    private final String message;

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
