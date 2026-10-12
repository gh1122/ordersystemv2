package com.gh.ordersystem.model.entity;

import java.time.LocalDateTime;

import lombok.Data;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("t_menu")
@Data
public class Menu {
  // id → Integer id
  // menu_name → String menuName
  // path → String path
  // icon → String icon
  // parent_id → Integer parentId ← 0 代表根节点
  // sort → Integer sort
  // type → Integer type ← 0目录 1菜单 2按钮
  // permission → String permission
  // create_time → LocalDateTime createTime
  private Integer id;
  private String menuName;
  private String path;
  private String icon;
  private Integer parentId;
  private Integer sort;
  private Integer type;
  private String permission;
  private LocalDateTime createTime;
}
