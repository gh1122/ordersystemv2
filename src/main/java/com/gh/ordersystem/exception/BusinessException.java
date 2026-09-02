package com.gh.ordersystem.exception;

import com.gh.ordersystem.model.enums.ErrorCode;
import lombok.Getter;

/**
 * 自定义业务异常
 *
 * 前端类比：
 * 就像前端封装的 ApiError 类：
 *
 *   class ApiError extends Error {
 *     constructor(public code: number, message: string) {
 *       super(message);
 *     }
 *   }
 *
 * 用法：throw new ApiError(1001, '用户名已存在')
 *
 * 后端也一样：
 * 用法：throw new BusinessException(ErrorCode.USERNAME_EXISTS)
 *
 * 为什么要自定义？
 * - 普通 Exception 没有错误码，只有消息
 * - BusinessException 携带 ErrorCode，方便全局异常处理器提取 code 和 message
 */
@Getter
public class BusinessException extends RuntimeException {

  // 错误码（比如 1001 = 用户名已存在）
  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());  // 把错误信息传给父类 RuntimeException
    this.errorCode = errorCode;
  }
}
