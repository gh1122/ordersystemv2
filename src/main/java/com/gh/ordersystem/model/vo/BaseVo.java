package com.gh.ordersystem.model.vo;

import lombok.Data;

@Data
public class BaseVo<T> {
  private boolean success;
  private String message;
  private T data;

  public static <T> BaseVo<T> success(T data) {
    BaseVo<T> baseVo = new BaseVo<>();
    baseVo.setSuccess(true);
    baseVo.setMessage("操作成功");
    baseVo.setData(data);
    return baseVo;
  }

  public static <T> BaseVo<T> error(String message) {
    BaseVo<T> baseVo = new BaseVo<>();
    baseVo.setSuccess(false);
    baseVo.setMessage(message);
    return baseVo;
  }

}
