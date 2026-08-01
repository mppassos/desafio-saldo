package com.itau.ingestor.service;

import java.time.Instant;

public final class TimestampConverter {

    private static final long MICROS_PER_SECOND = 1_000_000L;
    private static final long NANOS_PER_MICRO = 1_000L;

    private TimestampConverter() {

    }

    public static Instant fromMicros(long micros) {
        long seconds = Math.floorDiv(micros, MICROS_PER_SECOND);
        long microAdjustment = Math.floorMod(micros, MICROS_PER_SECOND);
        return Instant.ofEpochSecond(seconds, microAdjustment * NANOS_PER_MICRO);
    }

    public static Instant fromSeconds(String seconds) {
        return Instant.ofEpochSecond(Long.parseLong(seconds));
    }
}
