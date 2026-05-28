package com.agripath.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLoginVO {
    //id主键值
    private Long id;
    //用户名
    private String username;
    //token
    private String token;
}
