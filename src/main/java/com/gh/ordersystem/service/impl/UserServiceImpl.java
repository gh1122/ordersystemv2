package com.gh.ordersystem.service.impl;

import com.gh.ordersystem.repository.UserMapper;
import com.gh.ordersystem.service.UserService;
import com.gh.ordersystem.util.JwtUtil;
import com.gh.ordersystem.util.RedisUtil;
import com.gh.ordersystem.model.dto.UserLoginDTO;
import com.gh.ordersystem.model.dto.UserRegisterDTO;
import com.gh.ordersystem.model.vo.BaseVo;
import com.gh.ordersystem.model.vo.UserVo;

import com.gh.ordersystem.model.entity.User;
import com.gh.ordersystem.model.enums.ErrorCode;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
  @Autowired
  UserMapper UserMapper;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private RedisUtil redisUtil;

  @Override
  public BaseVo<Map<String, String>> login(UserLoginDTO dto) {
    // 1. 查用户
    User user = UserMapper.findByUsername(dto.getUsername());
    if (user == null) {
      return BaseVo.error(ErrorCode.USER_NOT_FOUND.getCode(),
          ErrorCode.USER_NOT_FOUND.getMessage());
    }

    // 2. 比对密码（BCrypt）
    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
      return BaseVo.error(ErrorCode.PASSWORD_ERROR.getCode(),
          ErrorCode.PASSWORD_ERROR.getMessage());
    }

    // 3. 签发 JWT
    String token = jwtUtil.createToken(user.getId(), user.getUsername());

    // ✅ 新增：把 userId 存到 Redis，key = "login:userId:1"
    // 24小时过期（和 JWT 一样）
    String redisKey = "login:userId:" + user.getId();
    redisUtil.set(redisKey, user.getId(), 24 * 60);

    // 4. 返回 token
    Map<String, String> data = new HashMap<>();
    data.put("token", token);
    return BaseVo.success(data);
    // String username = dto.getUsername();
    // String password = dto.getPassword();

    // User user = UserMapper.findByUsername(username);
    // if (user == null) {
    // return BaseVo.error(ErrorCode.USER_NOT_FOUND.getCode(),
    // ErrorCode.USER_NOT_FOUND.getMessage());
    // }

    // if (!passwordEncoder.matches(password, user.getPassword())) {
    // return BaseVo.error(ErrorCode.PASSWORD_ERROR.getCode(),
    // ErrorCode.PASSWORD_ERROR.getMessage());
    // }

    // // 生成 JWT token
    // String token = jwtUtil.generateToken(user);

    // Map<String, String> responseData = new HashMap<>();
    // responseData.put("token", token);

    // return BaseVo.success(responseData);
  }

  public BaseVo<UserVo> register(UserRegisterDTO UserRegisterDTO) {
    // 1.用户名不能为空 查询数据库是否有重复用户名
    // 2.密码不能为空 密码长度6-20位
    // 3.确认密码不能为空 确认密码和密码一致
    String username = UserRegisterDTO.getUsername();
    String password = UserRegisterDTO.getPassword();
    String confirmPassword = UserRegisterDTO.getConfirmPassword();

    // if (username == null || username.isEmpty()) {

    // // USERNAME_EMPTY
    // return BaseVo.error(ErrorCode.USERNAME_EMPTY.getCode(),
    // ErrorCode.USERNAME_EMPTY.getMessage());
    // }
    User existingUser = UserMapper.findByUsername(username);
    if (existingUser != null) {
      // 用户名已存在，返回错误信息或抛出异常
      return BaseVo.error(ErrorCode.USERNAME_EXISTS.getCode(),
          ErrorCode.USERNAME_EXISTS.getMessage());

    }

    // if (password == null || password.length() < 6 || password.length() > 20) {
    // return BaseVo.error(ErrorCode.PASSWORD_LENGTH_ERROR.getCode(),
    // ErrorCode.PASSWORD_LENGTH_ERROR.getMessage());
    // }
    if (!password.equals(confirmPassword)) {
      return BaseVo.error(ErrorCode.PASSWORD_NOT_MATCH.getCode(),
          ErrorCode.PASSWORD_NOT_MATCH.getMessage());
    }
    User newUser = new User();
    newUser.setUsername(username);
    newUser.setPassword(passwordEncoder.encode(password));
    UserMapper.insertUser(newUser);

    UserVo userVo = new UserVo();
    userVo.setId(newUser.getId());
    userVo.setUsername(newUser.getUsername());
    return BaseVo.success(userVo);
    // return BaseVo.success("注册成功");
  }

  @Override
  public BaseVo<String> logout(Integer userId) {
    // 删掉 Redis 里的登录态 → Token 立即失效
    String redisKey = "login:userId:" + userId;
    redisUtil.delete(redisKey);
    return BaseVo.success("退出登录成功");
  }
}
