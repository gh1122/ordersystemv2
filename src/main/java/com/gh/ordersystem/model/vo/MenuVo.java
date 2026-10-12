package com.gh.ordersystem.model.vo;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class MenuVo {
  private Integer id;
  private String menuName;
  private String path;
  private String icon;
  private Integer parentId;
  private Integer sort;
  private Integer type;
  private String permission;
  private LocalDateTime createTime;
  private List<MenuVo> children;
}
