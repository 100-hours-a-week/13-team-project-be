package com.matchimban.matchimban_api.auth.jwt;

public record GuestPrincipal(
        Long memberId,
        Long meetingId
) {}