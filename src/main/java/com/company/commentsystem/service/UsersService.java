package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Users;
import com.company.commentsystem.dao.repository.UsersRepository;
import com.company.commentsystem.utils.SuffixGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsersService {
    private final UsersRepository usersRepository;
    private final SuffixGenerator suffixGenerator = new SuffixGenerator();
    public String register(String fullName){
        Users user = new Users(fullName + "-" + suffixGenerator.generateName());
        return usersRepository.save(user).getFullName();
    }

}
