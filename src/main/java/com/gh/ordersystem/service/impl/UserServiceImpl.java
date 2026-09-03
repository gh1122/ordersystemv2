package com.gh.ordersystem.service.impl;

import com.gh.ordersystem.repository.UserMapper;
import com.gh.ordersystem.service.UserService;
import com.gh.ordersystem.model.dto.UserRegisterDTO;
import com.gh.ordersystem.model.vo.BaseVo;
import com.gh.ordersystem.model.vo.UserVo;

import com.gh.ordersystem.model.entity.User;
import com.gh.ordersystem.model.enums.ErrorCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
  @Autowired
  UserMapper UserMapper;

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
    newUser.setPassword(password);
    UserMapper.insertUser(newUser);

    UserVo userVo = new UserVo();
    userVo.setId(newUser.getId());
    userVo.setUsername(newUser.getUsername());
    return BaseVo.success(userVo);
    // return BaseVo.success("注册成功");
  }
}
