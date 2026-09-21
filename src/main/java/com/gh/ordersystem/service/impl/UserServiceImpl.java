package com.gh.ordersystem.service.impl;

import com.gh.ordersystem.repository.UserMapper;
import com.gh.ordersystem.service.UserService;
import com.gh.ordersystem.util.JwtUtil;
import com.gh.ordersystem.util.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gh.ordersystem.model.dto.UserLoginDTO;
import com.gh.ordersystem.model.dto.UserQueryDTO;
import com.gh.ordersystem.model.dto.UserRegisterDTO;
import com.gh.ordersystem.model.dto.UserUpdateDTO;
import com.gh.ordersystem.model.vo.BaseVo;
import com.gh.ordersystem.model.vo.UserVo;

import com.gh.ordersystem.model.entity.User;
import com.gh.ordersystem.model.enums.ErrorCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  @Override
  public BaseVo<List<UserVo>> getUserList() {
    List<User> users = UserMapper.selectList(null);
    // 2. Entity → VO 转换（脱敏：去掉密码等敏感字段）
    List<UserVo> voList = users.stream()
        .map(this::toUserVo)
        .collect(Collectors.toList());

    return BaseVo.success(voList);

  }

  @Override
  public BaseVo<Page<UserVo>> getUserPage(UserQueryDTO dto) {
    // 1. 创建分页对象（第几页，每页几条）
    Page<User> page = new Page<>(dto.getPageNum(), dto.getPageSize());

    // 2. 👇 构建查询条件（核心改动）
    QueryWrapper<User> wrapper = new QueryWrapper<>();

    // 如果 username 不为空，加模糊搜索条件
    if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
      wrapper.like("username", dto.getUsername());
    }

    // 按创建时间倒序
    wrapper.orderByDesc("create_time");

    // 2. 执行分页查询（传 null 表示没有查询条件）
    UserMapper.selectPage(page, wrapper);

    // 3. 转换成 VO 分页
    Page<UserVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    List<UserVo> voList = page.getRecords().stream()
        .map(this::toUserVo)
        .collect(Collectors.toList());
    voPage.setRecords(voList);

    return BaseVo.success(voPage);
  }

  @Override
  public BaseVo<String> updateUser(UserUpdateDTO dto) {
    // 1. 查用户是否存在
    User user = UserMapper.selectById(dto.getId());
    if (user == null) {
      return BaseVo.error(1005, "用户不存在");
    }

    // 2. 更新（只更新非空字段）
    if (dto.getUsername() != null) {
      user.setUsername(dto.getUsername());
    }

    // 3. 保存
    UserMapper.updateById(user);

    return BaseVo.success("修改成功");
  }

  @Override
  public BaseVo<String> deleteUser(Integer id) {
    // 1. 查用户是否存在
    User user = UserMapper.selectById(id);
    if (user == null) {
      return BaseVo.error(1005, "用户不存在");
    }

    // 2. 删除
    UserMapper.deleteById(id);

    return BaseVo.success("删除成功");
  }

  private UserVo toUserVo(User user) {
    UserVo vo = new UserVo();
    vo.setId(user.getId());
    vo.setUsername(user.getUsername());
    return vo;
  }
}
