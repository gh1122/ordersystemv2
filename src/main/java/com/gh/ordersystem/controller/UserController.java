package com.gh.ordersystem.controller;

import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gh.ordersystem.model.dto.UserLoginDTO;
import com.gh.ordersystem.model.dto.UserQueryDTO;
import com.gh.ordersystem.model.dto.UserRegisterDTO;
import com.gh.ordersystem.model.dto.UserUpdateDTO;
import com.gh.ordersystem.model.vo.BaseVo;
import com.gh.ordersystem.model.vo.UserVo;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.gh.ordersystem.service.UserService;
import com.gh.ordersystem.service.impl.UserServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class UserController {

  @Autowired
  UserService userService; // ✅ 依赖接口

  @PostMapping("/user/register")
  public BaseVo<UserVo> register(@RequestBody @Valid UserRegisterDTO UserRegisterDTO) {
    return userService.register(UserRegisterDTO);
  }

  @PostMapping("/login")
  public BaseVo<Map<String, String>> login(@Valid @RequestBody UserLoginDTO dto) {
    return userService.login(dto);
  }

  @PostMapping("/logout")
  public BaseVo<String> logout(HttpServletRequest request) {
    Integer userId = (Integer) request.getAttribute("userId");
    return userService.logout(userId);
  }

  @GetMapping("/user/list")
  public BaseVo<List<UserVo>> getUserList() {
    return userService.getUserList();
  }

  @GetMapping("/user/page")
  public BaseVo<Page<UserVo>> getUserPage(UserQueryDTO dto) {
    return userService.getUserPage(dto);
  }

  @PutMapping("/user/update")
  public BaseVo<String> updateUser(@RequestBody @Valid UserUpdateDTO dto) {
    return userService.updateUser(dto);
  }

  @DeleteMapping("/user/{id}")
  public BaseVo<String> deleteUser(@PathVariable Integer id) {
    return userService.deleteUser(id);
  }

}
