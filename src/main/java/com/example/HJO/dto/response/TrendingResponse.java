package com.example.HJO.dto.response;

import java.util.List;

/**
 * trending 결과. cursor 없이 상위 N개(기본 20)만 준다.
 */
public record TrendingResponse(List<TrendingPostResponse> items) {
}
