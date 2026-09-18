package com.example.HJO.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.HJO.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
