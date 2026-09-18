package com.example.HJO.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.HJO.domain.PostLike;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
}
