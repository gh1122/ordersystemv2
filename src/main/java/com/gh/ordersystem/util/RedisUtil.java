package com.gh.ordersystem.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  /**
   * 存值，带过期时间
   * 
   * @param key     键
   * @param value   值
   * @param minutes 过期分钟数
   */
  public void set(String key, Object value, long minutes) {
    // String.valueOf() 可以把任意类型转成 String，包括 null
    redisTemplate.opsForValue().set(key, String.valueOf(value), minutes, TimeUnit.MINUTES);
  }

  /**
   * 取值
   */
  public Object get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  /**
   * 删除
   */
  public void delete(String key) {
    redisTemplate.delete(key);
  }

  /**
   * 判断 key 是否存在
   */
  public boolean hasKey(String key) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
}
