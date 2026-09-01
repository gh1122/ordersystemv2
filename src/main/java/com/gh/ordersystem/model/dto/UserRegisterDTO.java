package com.gh.ordersystem.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDTO {
  @NotBlank
  private String username;

  @NotBlank
  @Size(min = 6, max = 20)
  private String password;

  @NotBlank
  private String confirmPassword;

}
