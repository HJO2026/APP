package com.example.HJO.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.HJO.domain.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {
}
