package com.gh.ordersystem.service;

import com.gh.ordersystem.model.vo.MenuVo;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gh.ordersystem.model.vo.BaseVo;

@Service
public interface MenuService {

  public BaseVo<List<MenuVo>> getMenuTree();

  // GET /menu/nav ，返回 BaseVo<List<MenuVo>>
  BaseVo<List<MenuVo>> getNavMenu(Integer userId);
  // BaseVo<UserVo> register(UserRegisterDTO dto);
}
