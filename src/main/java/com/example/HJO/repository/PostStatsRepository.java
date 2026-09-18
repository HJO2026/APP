package com.example.HJO.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.HJO.domain.PostStats;

public interface PostStatsRepository extends JpaRepository<PostStats, Long> {
}
