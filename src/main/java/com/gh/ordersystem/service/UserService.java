package com.gh.ordersystem.service;

// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gh.ordersystem.model.dto.UserRegisterDTO;
import com.gh.ordersystem.model.vo.BaseVo;
import com.gh.ordersystem.model.vo.UserVo;

@Service
public interface UserService {
  BaseVo<UserVo> register(UserRegisterDTO dto);
}
