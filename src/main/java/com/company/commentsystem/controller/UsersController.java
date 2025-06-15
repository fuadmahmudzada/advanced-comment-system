package com.company.commentsystem.controller;

import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.repository.UsersRepository;
import com.company.commentsystem.service.UsersService;
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
    private final UsersService usersService;

    @PostMapping
    public ResponseEntity<String> register(@RequestParam String username){
        String fullName = usersService.register(username);
        return ResponseEntity.status(HttpStatus.CREATED).body(String.format("User with name %s added", fullName));
    }
}
