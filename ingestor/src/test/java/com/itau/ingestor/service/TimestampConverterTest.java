package com.itau.ingestor.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class TimestampConverterTest {

    @Test
    void shouldConvertMicrosToInstant() {

        Instant expected = Instant.ofEpochSecond(1751641364L, 589_998_000L);

        Instant actual = TimestampConverter.fromMicros(1_751_641_364_589_998L);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldConvertMicrosWithRoundingDown() {

        assertThat(TimestampConverter.fromMicros(1_500_000L))
                .isEqualTo(Instant.ofEpochSecond(1, 500_000_000L));
    }

    @Test
    void shouldHandleNegativeMicrosWithoutTruncation() {

        assertThat(TimestampConverter.fromMicros(-1L))
                .isEqualTo(Instant.ofEpochSecond(-1, 999_999_000L));
    }

    @Test
    void shouldConvertSecondsStringToInstant() {
        assertThat(TimestampConverter.fromSeconds("1634874339"))
                .isEqualTo(Instant.ofEpochSecond(1_634_874_339L));
    }
}
