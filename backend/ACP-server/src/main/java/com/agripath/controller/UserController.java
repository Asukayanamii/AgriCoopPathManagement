package com.agripath.controller;

import com.agripath.dto.UserLoginDTO;
import com.agripath.result.Result;
import com.agripath.service.UserService;
import com.agripath.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录");
        UserLoginVO userLoginVO = userService.login(userLoginDTO);
        return Result.success(userLoginVO);
    }
    @GetMapping("/loginornot")
    public Result<UserLoginVO> loginornot() {
        log.info("验证到用户已登录");
        return Result.success();
    }
}
