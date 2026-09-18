package com.example.HJO.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 연관관계는 매핑하지 않고 FK 컬럼 값만 둔다. 조회는 네이티브 join 쿼리로 해서 지연 로딩 N+1이 생길 여지를 없앤다.
 * 카운터는 {@link PostStats}에 있다.
 */
@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "board_id", nullable = false)
	private Long boardId;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	protected Post() {
	}

	public Post(Long boardId, Long authorId, String title, String content) {
		this.boardId = boardId;
		this.authorId = authorId;
		this.title = title;
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public Long getBoardId() {
		return boardId;
	}

	public Long getAuthorId() {
		return authorId;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

}
