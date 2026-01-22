package com.matchimban.matchimban_api.member.mapper;

import com.matchimban.matchimban_api.member.dto.MemberCreateRequest;
import com.matchimban.matchimban_api.member.entity.Member;
import com.matchimban.matchimban_api.member.entity.enums.MemberStatus;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

	public Member toMember(MemberCreateRequest request) {
		LocalDateTime now = LocalDateTime.now();
		return Member.builder()
			.nickname(request.nickname())
			.profileImageUrl(request.profileImageUrl())
			.thumbnailImageUrl(request.thumbnailImageUrl())
			.status(MemberStatus.PENDING)
			.createdAt(now)
			.updatedAt(now)
			.isGuest(false)
			.guestUuid(null)
			.build();
	}
}
