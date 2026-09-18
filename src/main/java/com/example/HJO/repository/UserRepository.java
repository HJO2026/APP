package com.example.HJO.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.HJO.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
