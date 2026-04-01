package com.itheima.dormrepairsystemspringboot.interceptor;

import com.itheima.dormrepairsystemspringboot.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        System.out.println("拦截器检查路径: " + uri + ", 方法: " + method);

        //OPTIONS 预检请求直接放行
        //浏览器预检请求，用于检查跨域，不带 token
        if ("OPTIONS".equals(method)) {
            System.out.println("拦截器: OPTIONS请求，直接放行");
            return true;
        }

        //登录和注册接口直接放行
        if (uri.contains("login") || uri.contains("register")) {
            System.out.println("拦截器: 登录/注册接口，直接放行");
            return true;
        }

        //图片访问放行
        if (uri.startsWith("/upload")) {
            System.out.println("拦截器: 图片访问，直接放行");
            return true;
        }

        //从请求头获取 token
        String token = request.getHeader("token");

        //未携带 token
        if (token == null || token.isEmpty()) {
            System.out.println("拦截器: 未携带token，返回401");
            response.setStatus(401);
            return false;
        }

        //验证 token
        try {
            Claims claims = JwtUtil.parseToken(token);

            // 检查是否过期
            if (JwtUtil.isExpired(token)) {
                System.out.println("拦截器: token已过期，返回401");
                response.setStatus(401);
                return false;
            }

            System.out.println("拦截器: token验证通过，用户: " + claims.get("account"));
            return true;

        } catch (Exception e) {
            System.out.println("拦截器: token解析失败: " + e.getMessage());
            response.setStatus(401);
            return false;
        }
    }
}