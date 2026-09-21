package com.gh.ordersystem.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.gh.ordersystem.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; //

@Mapper
public interface UserMapper extends BaseMapper<User> {

  @Insert("INSERT INTO t_user (username, password) VALUES (#{username}, #{password})")
  @Options(useGeneratedKeys = true, keyProperty = "id") // 设置自增主键
  void insertUser(User user);

  @Select("SELECT * FROM t_user WHERE username = #{username}")
  User findByUsername(String username);

  // @Select("SELECT * FROM t_user")
  // List<User> selectList();
}
