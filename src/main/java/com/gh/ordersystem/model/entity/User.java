package com.gh.ordersystem.model.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("t_user")
@Data
public class User {
  private int id;
  private String username;
  private String password;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

}
