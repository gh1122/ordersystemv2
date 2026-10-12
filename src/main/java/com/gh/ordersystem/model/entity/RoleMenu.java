package com.gh.ordersystem.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("t_role_menu")
@Data
public class RoleMenu {
  private Integer id;
  private Integer roleId;
  private Integer menuId;
  private LocalDateTime createTime;
}
