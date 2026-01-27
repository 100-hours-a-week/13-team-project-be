package com.matchimban.matchimban_api.meeting.repository.projection;

public interface MeetingParticipantProfileRow {
    Long getMemberId();
    String getNickname();
    String getProfileImageUrl();
}