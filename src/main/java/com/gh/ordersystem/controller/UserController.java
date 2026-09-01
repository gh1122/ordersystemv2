package com.gh.ordersystem.controller;

import org.springframework.web.bind.annotation.RestController;

import com.gh.ordersystem.model.dto.UserRegisterDTO;
import com.gh.ordersystem.model.vo.BaseVo;
import com.gh.ordersystem.model.vo.UserVo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.AccessType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.gh.ordersystem.service.UserService;

@RestController
public class UserController {

  @Autowired
  UserService UserService;

  @PostMapping("/user/register")
  public BaseVo<UserVo> register(@RequestBody UserRegisterDTO UserRegisterDTO) {
    return UserService.register(UserRegisterDTO);
  }

}
