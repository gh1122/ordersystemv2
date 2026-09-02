package com.gh.ordersystem.exception;

import com.gh.ordersystem.model.enums.ErrorCode;
import com.gh.ordersystem.model.vo.BaseVo;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * 前端类比：
 * 就像 React 的 ErrorBoundary 组件 或 axios 的响应拦截器：
 *
 *   // axios 响应拦截器
 *   axios.interceptors.response.use(
 *     response => response.data,
 *     error => {
 *       // 不管哪个接口报错，都统一格式返回
 *       return Promise.resolve({
 *         code: error.response.status,
 *         success: false,
 *         message: error.response.data.message,
 *         data: null
 *       });
 *     }
 *   )
 *
 * @RestControllerAdvice 的作用：
 * 拦截所有 @RestController 抛出的异常，统一处理返回格式
 *
 * 好处：
 * - Controller / Service 里不需要 try-catch，直接 throw 就行
 * - 所有接口的错误返回格式统一为 BaseVo
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 处理参数校验失败（@Valid 触发）
   *
   * 场景：前端传的参数不符合 DTO 上的注解规则
   *   - @NotBlank 字段为空
   *   - @Size 长度不对
   *
   * 前端类比：
   * 就像前端表单校验失败时，把 input 里的红色提示返回给调用方
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public BaseVo<?> handleValidation(MethodArgumentNotValidException e) {
    // 拿到第一个校验失败的字段信息
    // getFieldError() 返回哪个字段先出错
    // getDefaultMessage() 返回注解里写的 message（比如 "用户名不能为空"）
    String specificMsg = e.getBindingResult().getFieldError().getDefaultMessage();

    // 返回 400 + 具体错误信息
    return BaseVo.error(ErrorCode.PARAM_ERROR.getCode(), specificMsg);
  }

  /**
   * 处理业务异常
   *
   * 场景：业务规则不通过（比如用户名已存在）
   * Service 里主动抛出：throw new BusinessException(ErrorCode.USERNAME_EXISTS)
   *
   * 前端类比：
   * 就像前端在提交表单前检查用户名是否重复，发现重复后：
   *   throw new ApiError(1001, '用户名已存在')
   */
  @ExceptionHandler(BusinessException.class)
  public BaseVo<?> handleBusiness(BusinessException e) {
    // 从异常对象里提取错误码和信息
    return BaseVo.error(e.getErrorCode().getCode(), e.getMessage());
  }

  /**
   * 兜底：处理所有未被捕获的异常
   *
   * 场景：代码 bug（空指针、数据库连接失败等）
   *
   * 前端类比：
   * 就像 axios 拦截器里的兜底：
   *   catch (error) {
   *     message.error('系统异常，请稍后重试');
   *   }
   *
   * 注意：生产环境应该记录日志，但不要返回详细错误给前端（防止泄露敏感信息）
   */
  @ExceptionHandler(Exception.class)
  public BaseVo<?> handleOther(Exception e) {
    e.printStackTrace();  // 控制台打印堆栈，方便调试
    return BaseVo.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
  }
}
