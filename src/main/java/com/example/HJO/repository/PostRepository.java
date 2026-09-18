package com.example.HJO.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.HJO.domain.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}
