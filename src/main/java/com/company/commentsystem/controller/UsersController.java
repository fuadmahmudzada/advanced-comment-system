package com.company.commentsystem.controller;

import com.company.commentsystem.service.UserService;
import com.company.commentsystem.utils.ResponseUtil;
import com.company.commentsystem.model.dto.response.ApiResponse;
import com.company.commentsystem.service.impl.UsersServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("v1/users")
public class UsersController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> register(@RequestParam String username){
        String fullName = userService.register(username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success(String.format("User with name %s added", fullName), null, null));
    }
}
