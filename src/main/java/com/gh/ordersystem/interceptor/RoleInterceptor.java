package com.gh.ordersystem.interceptor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gh.ordersystem.annotation.RequireRole;
import com.gh.ordersystem.model.entity.Role;
import com.gh.ordersystem.model.entity.UserRole;
import com.gh.ordersystem.repository.RoleMapper;
import com.gh.ordersystem.repository.UserRoleMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 角色权限拦截器 —— 在 JwtInterceptor 之后执行
 *
 * 前端类比：就像路由守卫里的权限判断
 *   if (route.meta.roles && !route.meta.roles.includes(user.role)) {
 *     return next('/403');
 *   }
 *
 * 执行顺序（WebMvcConfig 里按注册顺序）：
 *   JwtInterceptor（登录了吗？）→ RoleInterceptor（有权限吗？）→ Controller
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

  @Autowired
  private UserRoleMapper userRoleMapper;

  @Autowired
  private RoleMapper roleMapper;

  @Override
  public boolean preHandle(HttpServletRequest request,
      HttpServletResponse response,
      Object handler) throws Exception {

    // 1. 把 handler 强转成 HandlerMethod（能拿到"这次请求要调的那个方法"）
    // 不是 HandlerMethod 说明请求的是静态资源（.js/.css），没有注解可查 → 放行
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    // 2. 反射取方法上的 @RequireRole 注解；没有 → 放行（return true，大部分接口不需要角色）
    RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
    if (requireRole == null) {
      return true;
    }

    // 3. 有 → 取 userId（request.getAttribute("userId")）
    // 为空 → 403（说明请求没经过 JwtInterceptor，配置有问题）
    Object userIdAttr = request.getAttribute("userId");
    if (userIdAttr == null) {
      write403(response, "权限校验失败：未获取到用户信息");
      return false;
    }
    Integer userId = (Integer) userIdAttr;

    // 4. 查角色名链路：userId → t_user_role → roleIds → t_role（id in roleIds）→ roleNames
    List<UserRole> userRoles = userRoleMapper.selectList(
        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
    if (userRoles.isEmpty()) {
      write403(response, "无权限访问该接口");
      return false;
    }

    List<Integer> roleIds = userRoles.stream()
        .map(UserRole::getRoleId)
        .collect(Collectors.toList());

    List<Role> roles = roleMapper.selectList(
        new LambdaQueryWrapper<Role>().in(Role::getId, roleIds));

    Set<String> roleNames = roles.stream()
        .map(Role::getRoleName)
        .collect(Collectors.toSet());

    // 5. roleNames 和注解的 value() 有交集 → 放行；否则 → 返回 403 JSON
    Set<String> allowedRoles = Set.of(requireRole.value());
    boolean hasPermission = roleNames.stream().anyMatch(allowedRoles::contains);
    if (!hasPermission) {
      write403(response, "无权限访问该接口");
      return false;
    }

    return true;
  }

  /**
   * 返回 403 JSON
   *
   * 为什么拦截器里要手动写 JSON，而不是抛异常走 GlobalExceptionHandler？
   * 因为 @RestControllerAdvice 只拦截 Controller 抛出的异常，
   * 拦截器阶段还没进 Controller，只能自己写响应 —— 和 JwtInterceptor 返回 401 同理。
   */
  private void write403(HttpServletResponse response, String message) throws Exception {
    response.setStatus(403);
    response.setContentType("application/json;charset=UTF-8");
    // 这里不拼接 message 变量，固定文案，避免引号转义问题
    response.getWriter().write("{\"code\":403,\"success\":false,\"message\":\"无权限访问该接口\",\"data\":null}");
  }
}
