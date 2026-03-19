package com.matchimban.matchimban_api.event.entity;

public enum CouponType {
    SUPER_LIKE;

    public String getDisplayName() {
        return switch (this) {
            case SUPER_LIKE -> "슈퍼라이크 쿠폰";
        };
    }
}
