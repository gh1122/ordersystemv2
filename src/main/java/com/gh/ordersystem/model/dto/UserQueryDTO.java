package com.gh.ordersystem.model.dto;

import lombok.Data;

@Data
public class UserQueryDTO {
  private Integer pageNum = 1; // 第几页，默认第 1 页
  private Integer pageSize = 10; // 每页几条，默认 10 条

  private String username; // 按用户名模糊搜索

}
