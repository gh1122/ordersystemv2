package com.gh.ordersystem.repository;

import org.apache.ibatis.annotations.Mapper;

import com.gh.ordersystem.model.entity.UserRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

}
