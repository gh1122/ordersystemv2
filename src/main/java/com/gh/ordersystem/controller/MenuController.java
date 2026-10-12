package com.gh.ordersystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gh.ordersystem.model.vo.MenuVo;
import com.gh.ordersystem.service.MenuService;

import java.util.List;

import com.gh.ordersystem.model.vo.BaseVo;

@RestController
@RequestMapping("/menu")
public class MenuController {
  @Autowired
  private MenuService menuService;

  // GET /menu/tree ，返回 BaseVo<List<MenuVo>>
  @GetMapping("/tree")
  public BaseVo<List<MenuVo>> getMenuTree() {
    return menuService.getMenuTree();
  }
}
