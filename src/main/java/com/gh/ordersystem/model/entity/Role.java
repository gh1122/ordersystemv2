package com.gh.ordersystem.model.entity;

import java.time.LocalDateTime;
import lombok.Data;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("t_role")
@Data
public class Role {
  private Integer id;
  private String roleName;
  private String description;
  private Integer status;
  private LocalDateTime createTime;

}
