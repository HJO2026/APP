package com.example.HJO.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.HJO.domain.PostViewEvent;

public interface PostViewEventRepository extends JpaRepository<PostViewEvent, Long> {
}
