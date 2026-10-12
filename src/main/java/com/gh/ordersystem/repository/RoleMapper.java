package com.gh.ordersystem.repository;

import org.apache.ibatis.annotations.Mapper;

import com.gh.ordersystem.model.entity.Role;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

}
