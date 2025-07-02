package com.scalefocus.blogapplication.mapper;

import com.scalefocus.blogapplication.dto.UserResponse;
import com.scalefocus.blogapplication.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "username", source = "username")
    @Mapping(target = "accountNonLocked", source = "accountNonLocked")
    @Mapping(target = "authority", source = "authority")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "autoLockedAt", source = "autoLockedAt")
    @Mapping(target = "loginAttempts", source = "loginAttempts")
    UserResponse userToUserResponse(User user);
}
