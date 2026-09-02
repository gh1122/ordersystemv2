package com.gh.ordersystem.model.vo;

import lombok.Data;
import com.gh.ordersystem.model.enums.ErrorCode;

/**
 * 统一响应包装类（所有接口返回这个格式）
 *
 * 前端类比：
 * 就像你们约定的后端返回格式：
 *   interface ApiResponse<T> {
 *     code: number;
 *     success: boolean;
 *     message: string;
 *     data: T;  ← T 是泛型，根据接口不同变成不同数据类型
 *   }
 *
 * 用法：
 *   ApiResponse<UserVo>        → data 是用户信息
 *   ApiResponse<List<OrderVo>> → data 是订单数组
 *   ApiResponse<string>        → data 是字符串
 *
 * Java 的 <T> 就是 TypeScript 的 <T>，一个"类型占位符"
 */
@Data
public class BaseVo<T> {

  private int code;
  private boolean success;
  private String message;
  private T data;  // T 的具体类型由调用方决定

  /**
   * 成功时调用
   *
   * 方法签名拆解：public static <T> BaseVo<T> success(T data)
   *
   *   public static      → 静态方法，直接 BaseVo.success() 调用，不用 new
   *   <T>                ← 【第一个T】声明"这个方法要用泛型T"
   *                       类比 TS: function success<T>(data: T): ApiResponse<T>
   *
   *   BaseVo<T>          ← 【返回值】返回一个 BaseVo，里面的 data 类型是 T
   *
   *   success(T data)    ← 【参数】接收一个 T 类型的参数
   *
   * 为什么第一个 <T> 不能省？
   * 因为 static 方法没有类的泛型（类上的 <T> 是实例级别的），
   * 所以必须在方法上自己声明 <T>，告诉编译器"T是个类型变量"。
   *
   * 类比 TS：
   *   // ❌ 不声明 T，编译器不知道 T 是什么
   *   function success(data: T) { ... }
   *
   *   // ✅ 声明了 <T>，才知道 T 是泛型
   *   function success<T>(data: T): ApiResponse<T> { ... }
   */
  public static <T> BaseVo<T> success(T data) {
    BaseVo<T> baseVo = new BaseVo<>();
    baseVo.setCode(ErrorCode.SUCCESS.getCode());
    baseVo.setSuccess(true);
    baseVo.setMessage(ErrorCode.SUCCESS.getMessage());
    baseVo.setData(data);
    return baseVo;
  }

  /**
   * 失败时调用
   *
   * 同理：public static <T> BaseVo<T> error(int code, String message)
   *   <T>           → 声明泛型
   *   BaseVo<T>     → 返回值类型
   *   error(int, String) → 参数（error 不需要 data，所以没 T 参数）
   *
   * 虽然 error 方法里没用到 T（data 是 null），
   * 但返回值 BaseVo<T> 需要和 success 保持一致，方便调用方统一接收。
   */
  public static <T> BaseVo<T> error(int errorCode, String message) {
    BaseVo<T> baseVo = new BaseVo<>();
    baseVo.setSuccess(false);
    baseVo.setCode(errorCode);
    baseVo.setMessage(message);
    return baseVo;
  }
}
