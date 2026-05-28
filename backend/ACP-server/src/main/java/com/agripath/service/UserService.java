package com.agripath.service;

import com.agripath.dto.UserLoginDTO;
import com.agripath.vo.UserLoginVO;

public interface UserService {
    UserLoginVO login(UserLoginDTO userLoginDTO);
}
