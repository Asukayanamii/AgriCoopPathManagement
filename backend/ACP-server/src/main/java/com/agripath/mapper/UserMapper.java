package com.agripath.mapper;

import com.agripath.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    User getByUsername(String username);
}
