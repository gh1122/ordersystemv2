package com.gh.ordersystem.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateDTO {
  @NotNull(message = "用户ID不能为空")
  private Integer id;

  private String username;
}
