package com.gh.ordersystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gh.ordersystem.model.vo.MenuVo;

import java.util.List;

import com.gh.ordersystem.model.vo.BaseVo;

@RestController
@RequestMapping("/menu")
public class MenuController {
  // GET /menu/tree ，返回 BaseVo<List<MenuVo>>
  @GetMapping("/tree")
  public BaseVo<List<MenuVo>> getMenuTree() {
    return null;
  }
}
