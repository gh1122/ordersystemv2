package com.gh.ordersystem.service;

import java.util.List;
import java.util.Map;

// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gh.ordersystem.model.dto.UserLoginDTO;
import com.gh.ordersystem.model.dto.UserQueryDTO;
import com.gh.ordersystem.model.dto.UserRegisterDTO;
import com.gh.ordersystem.model.dto.UserUpdateDTO;
import com.gh.ordersystem.model.vo.BaseVo;
import com.gh.ordersystem.model.vo.UserVo;

@Service
public interface UserService {
  BaseVo<UserVo> register(UserRegisterDTO dto);

  BaseVo<Map<String, String>> login(UserLoginDTO dto);

  BaseVo<String> logout(Integer userId);

  BaseVo<List<UserVo>> getUserList();

  BaseVo<Page<UserVo>> getUserPage(UserQueryDTO dto);

  BaseVo<String> updateUser(UserUpdateDTO dto);

  BaseVo<String> deleteUser(Integer id);
}
