package com.grace.train12306.biz.userservice.toolkit;

import java.util.regex.Pattern;

public class RegexUtil {
    public static final String PHONE_REGEX = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";

    public static final String IDCARD_REGEX = "^([1-6][1-9]|50)\\d{4}(18|19|20)\\d{2}((0[1-9])|10|11|12)(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";

    public static boolean isValidPhone (String phone) {
        return Pattern.matches(PHONE_REGEX, phone);
    }

    public static boolean isValidIdCard (String idCard) {
        return Pattern.matches(IDCARD_REGEX, idCard);
    }
}
