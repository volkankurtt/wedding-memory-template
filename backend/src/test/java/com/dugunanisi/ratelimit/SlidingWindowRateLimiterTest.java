package com.dugunanisi.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class SlidingWindowRateLimiterTest {

	@Test
	void allowsUpToLimitThenRejectsUntilWindowPasses() {
		MutableClock clock = new MutableClock(Instant.parse("2026-09-07T00:00:00Z"));
		SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(clock);
		Duration window = Duration.ofMinutes(10);

		assertThat(limiter.tryAcquire("photos:1.1.1.1", 2, window)).isTrue();
		assertThat(limiter.tryAcquire("photos:1.1.1.1", 2, window)).isTrue();
		assertThat(limiter.tryAcquire("photos:1.1.1.1", 2, window)).isFalse();
		assertThat(limiter.tryAcquire("photos:2.2.2.2", 2, window)).isTrue();

		clock.setInstant(Instant.parse("2026-09-07T00:10:01Z"));
		assertThat(limiter.tryAcquire("photos:1.1.1.1", 2, window)).isTrue();
	}

	private static final class MutableClock extends Clock {
		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		void setInstant(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
