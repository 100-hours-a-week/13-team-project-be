package com.matchimban.matchimban_api.global.time;

import java.time.*;

public final class TimeKst {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TimeKst() {}

    public static Instant toInstantFromKst(LocalDateTime kstLocalDateTime) {
        if (kstLocalDateTime == null) return null;
        return kstLocalDateTime.atZone(KST).toInstant();
    }

    public static LocalDateTime toKstLocalDateTime(Instant instant) {
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, KST);
    }

    public static boolean isFutureInKst(LocalDateTime kstLocalDateTime) {
        if (kstLocalDateTime == null) return false;
        LocalDateTime nowKst = LocalDateTime.now(KST);
        return kstLocalDateTime.isAfter(nowKst);
    }
}
