package com.gh.ordersystem.repository;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.gh.ordersystem.model.entity.User;

@Mapper
public interface UserMapper {

  @Insert("INSERT INTO t_user (username, password) VALUES (#{username}, #{password})")
  @Options(useGeneratedKeys = true, keyProperty = "id") // 设置自增主键
  void insertUser(User user);

  @Select("SELECT * FROM t_user WHERE username = #{username}")
  User findByUsername(String username);

}
