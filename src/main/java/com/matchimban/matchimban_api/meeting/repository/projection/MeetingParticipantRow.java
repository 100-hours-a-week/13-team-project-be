package com.matchimban.matchimban_api.meeting.repository.projection;

import com.matchimban.matchimban_api.meeting.entity.MeetingParticipant;

import java.time.LocalDateTime;

public interface MeetingParticipantRow {
    Long getMemberId();
    MeetingParticipant.Role getRole();
    MeetingParticipant.Status getStatus();
    LocalDateTime getCreatedAt();
}