package com.gh.ordersystem.interceptor;

import com.gh.ordersystem.util.JwtUtil;
import com.gh.ordersystem.util.RedisUtil;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private RedisUtil redisUtil;

  @Override
  public boolean preHandle(HttpServletRequest request,
      HttpServletResponse response,
      Object handler) throws Exception {

    // 1. 取 Authorization header
    String authHeader = request.getHeader("Authorization");

    // 2. 没有 header 或不是 Bearer 开头 → 401
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      response.setStatus(401);
      response.getWriter().write("未登录或Token无效");
      return false; // 不放行
    }
    // 3. 去掉 "Bearer " 前缀，拿到纯 token
    String token = authHeader.substring(7);

    // 4. 验证 token
    try {

      // 把 userId 存到 request 里，Controller 可以取
      Claims claims = jwtUtil.parseToken(token);
      Integer userId = (Integer) claims.get("userId");

      // ✅ 新增：Redis 里还有这个用户吗？
      String redisKey = "login:userId:" + userId;
      if (!redisUtil.hasKey(redisKey)) {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"登录已失效，请重新登录\"}");
        return false;
      }

      request.setAttribute("userId", claims.get("userId"));
      return true; // 放行
    } catch (Exception e) {
      response.setStatus(401);
      response.getWriter().write("Token无效或已过期");
      return false;
    }
  }
}