package com.grace.train12306.framework.starter.user.toolkit;

import com.alibaba.fastjson2.JSON;
import com.grace.train12306.framework.starter.user.core.UserInfoDTO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.grace.train12306.framework.starter.bases.constant.UserConstant.*;


/**
 * JWT 工具类
 */
@Slf4j
public final class JWTUtil {

    static {
        KEY = System.getenv("JWT_KEY");
    }

    private static final long EXPIRATION = 86400L; // 24h有效期
    public static final String ISS = "train12306";
    public static final String KEY;

    /**
     * 生成用户 Token
     *
     * @param userInfo 用户信息
     * @return 用户访问 Token
     */
    public static String generateAccessToken(UserInfoDTO userInfo) {
        Map<String, Object> customerUserMap = new HashMap<>();
        customerUserMap.put(USER_ID_KEY, userInfo.getUserId());
        customerUserMap.put(USER_NAME_KEY, userInfo.getUsername());
        customerUserMap.put(REAL_NAME_KEY, userInfo.getRealName());
        String jwtToken = Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, KEY)
                .setIssuedAt(new Date())
                .setIssuer(ISS)
                .setSubject(JSON.toJSONString(customerUserMap))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION * 1000))
                .compact();
        return jwtToken;
    }
}