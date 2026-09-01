package com.gh.ordersystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gh.ordersystem.model.dto.UserRegisterDTO;
import com.gh.ordersystem.model.vo.BaseVo;
import com.gh.ordersystem.model.vo.UserVo;
import com.gh.ordersystem.repository.UserMapper;
import com.gh.ordersystem.model.entry.User;
import com.gh.ordersystem.model.vo.BaseVo;

@Service
public class UserService {

  @Autowired
  UserMapper UserMapper;

  public BaseVo<UserVo> register(UserRegisterDTO UserRegisterDTO) {
    // 1.用户名不能为空 查询数据库是否有重复用户名
    // 2.密码不能为空 密码长度6-20位
    // 3.确认密码不能为空 确认密码和密码一致
    String username = UserRegisterDTO.getUsername();
    String password = UserRegisterDTO.getPassword();
    String confirmPassword = UserRegisterDTO.getConfirmPassword();

    User existingUser = UserMapper.findByUsername(username);
    if (existingUser != null) {
      // 用户名已存在，返回错误信息或抛出异常
      return BaseVo.error("用户名已存在");
    }
    if (username == null || username.isEmpty()) {
      return BaseVo.error("用户名不能为空");
    }
    if (password == null || password.length() < 6 || password.length() > 20) {
      return BaseVo.error("密码长度必须在6到20位之间");
    }
    if (!password.equals(confirmPassword)) {
      return BaseVo.error("确认密码与密码不一致");
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
