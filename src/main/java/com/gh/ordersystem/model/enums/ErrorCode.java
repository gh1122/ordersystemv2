package com.gh.ordersystem.model.enums;

public enum ErrorCode {
  // 通用
  SUCCESS(200, "操作成功"),
  PARAM_ERROR(400, "参数错误"),
  SYSTEM_ERROR(500, "系统异常"),

  // 用户模块 10xx
  USERNAME_EXISTS(1001, "用户名已存在"),
  USERNAME_EMPTY(1002, "用户名不能为空"),
  PASSWORD_LENGTH_ERROR(1003, "密码长度必须在6到20位之间"),
  PASSWORD_NOT_MATCH(1004, "确认密码与密码不一致");

  private final int code;
  private final String message;

  ErrorCode(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
