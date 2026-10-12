package com.gh.ordersystem.model.entity;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("t_user_role")
@Data
public class UserRole {
  private Integer id;
  private Integer userId;
  private Integer roleId;
}
