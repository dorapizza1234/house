package com.example.house.dto;

import java.time.LocalDateTime;

public record DashboardMemberDto(
		  Long memberId,
		  String nickname,
	      String presenceStatus,
	      LocalDateTime presenceUpdatedAt
	  ) {}
