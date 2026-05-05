package com.example.house.dto;

import java.time.LocalDateTime;

public record DashboardMemberDto(
		  String nickname,
	      String presenceStatus,
	      LocalDateTime presenceUpdatedAt
	  ) {}