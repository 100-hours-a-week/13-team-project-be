package com.matchimban.matchimban_api.meeting.repository;

import com.matchimban.matchimban_api.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    boolean existsByInviteCode(String inviteCode);

    Optional<Meeting> findByInviteCodeAndIsDeletedFalse(String inviteCode);

    List<Meeting> findByIdIn(List<Long> ids);

    Optional<Meeting> findByIdAndIsDeletedFalse(Long id);
}
