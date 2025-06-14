package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsersService {
    private final UsersRepository usersRepository;

    public String register(String fullName){
        return usersRepository.save(new Users(fullName)).getFullName();
    }

}
