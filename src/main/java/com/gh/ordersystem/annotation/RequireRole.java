package com.gh.ordersystem.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 只能用于方法
@Retention(RetentionPolicy.RUNTIME) // 保留到运行时（运行时才会读取注解）
public @interface RequireRole {
  String[] value() default {}; // 允许的角色数组
}
