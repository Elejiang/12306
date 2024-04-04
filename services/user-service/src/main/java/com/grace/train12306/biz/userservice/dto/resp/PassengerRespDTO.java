package com.grace.train12306.biz.userservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.grace.train12306.biz.userservice.serialize.IdCardDesensitizationSerializer;
import com.grace.train12306.biz.userservice.serialize.PhoneDesensitizationSerializer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 乘车人返回参数
 */
@Data
@Accessors(chain = true)
public class PassengerRespDTO {

    /**
     * 乘车人id
     */
    private String id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号码
     */
    @JsonSerialize(using = IdCardDesensitizationSerializer.class)
    private String idCard;

    /**
     * 真实证件号码
     */
    private String actualIdCard;

    /**
     * 优惠类型
     */
    private Integer discountType;

    /**
     * 手机号
     */
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;

    /**
     * 真实手机号
     */
    private String actualPhone;

    /**
     * 添加日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createDate;

    /**
     * 审核状态
     */
    private Integer verifyStatus;
}
