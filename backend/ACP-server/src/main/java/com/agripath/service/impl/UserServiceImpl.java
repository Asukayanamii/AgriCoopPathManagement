package com.agripath.service.impl;

import com.agripath.constent.JwtClaimsConstant;
import com.agripath.constent.MessageConstant;
import com.agripath.dto.UserLoginDTO;
import com.agripath.entity.User;
import com.agripath.exception.AccountNotFoundException;
import com.agripath.exception.PasswordErrorException;
import com.agripath.mapper.UserMapper;
import com.agripath.properties.JwtProperties;
import com.agripath.service.UserService;
import com.agripath.utils.JwtUtil;
import com.agripath.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtProperties jwtProperties;
    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        log.info("用户登录");
        //根据用户名查询用户
        User user = userMapper.getByUsername(userLoginDTO.getUsername());
        //用户不存在
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        //密码对比
        // 对前端传过来的明文密码进行md5加密
        String password = DigestUtils.md5DigestAsHex(userLoginDTO.getPassword().getBytes());
        if (!password.equals(user.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        //登录成功，返回用户信息和token
        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);
        //封装对象
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
        return userLoginVO;
    }
}
