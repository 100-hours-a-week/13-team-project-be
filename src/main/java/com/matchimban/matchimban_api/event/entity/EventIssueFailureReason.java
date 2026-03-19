package com.matchimban.matchimban_api.event.entity;

public enum EventIssueFailureReason {
    EVENT_NOT_STARTED,
    EVENT_ENDED,
    ALREADY_ISSUED,
    ALREADY_WAITING,
    QUEUE_LIMIT_EXCEEDED,
    SOLD_OUT,
    SYSTEM_ERROR
}
