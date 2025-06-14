package com.company.commentsystem.dao.repository;

import com.company.commentsystem.dao.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, Long> {
}
