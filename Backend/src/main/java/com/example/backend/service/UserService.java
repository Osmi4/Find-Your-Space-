package com.example.backend.service;

import com.example.backend.dtos.User.UpdateUserRequest;
import com.example.backend.dtos.User.UserFilter;
import com.example.backend.dtos.User.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getUser(String id);
    List<UserResponse> getUsers(UserFilter userFilter);

    UserResponse updateUser(UpdateUserRequest updateUserRequest);
}
