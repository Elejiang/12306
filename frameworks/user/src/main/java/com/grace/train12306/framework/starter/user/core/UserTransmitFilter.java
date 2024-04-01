package com.grace.train12306.framework.starter.user.core;

import com.grace.train12306.framework.starter.bases.constant.UserConstant;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 用户信息传输过滤器
 */
public class UserTransmitFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String userId = httpServletRequest.getHeader(UserConstant.USER_ID_KEY);
        if (StringUtils.hasText(userId)) {
            String userName = httpServletRequest.getHeader(UserConstant.USER_NAME_KEY);
            String realName = httpServletRequest.getHeader(UserConstant.REAL_NAME_KEY);
            if (StringUtils.hasText(userName)) {
                userName = URLDecoder.decode(userName, UTF_8);
            }
            if (StringUtils.hasText(realName)) {
                realName = URLDecoder.decode(realName, UTF_8);
            }
            String token = httpServletRequest.getHeader(UserConstant.USER_TOKEN_KEY);
            UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                    .userId(userId)
                    .username(userName)
                    .realName(realName)
                    .token(token)
                    .build();
            UserContext.setUser(userInfoDTO);
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }
}
