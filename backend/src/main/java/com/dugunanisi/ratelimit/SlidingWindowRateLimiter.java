package com.dugunanisi.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowRateLimiter {

	private final ConcurrentHashMap<String, Deque<Long>> windows = new ConcurrentHashMap<>();
	private final Clock clock;

	public SlidingWindowRateLimiter() {
		this(Clock.systemUTC());
	}

	public SlidingWindowRateLimiter(Clock clock) {
		this.clock = clock;
	}

	public boolean tryAcquire(String key, int limit, Duration window) {
		long now = clock.millis();
		long cutoff = now - window.toMillis();
		Deque<Long> stamps = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
		synchronized (stamps) {
			while (!stamps.isEmpty() && stamps.peekFirst() <= cutoff) {
				stamps.removeFirst();
			}
			if (stamps.size() >= limit) {
				return false;
			}
			stamps.addLast(now);
			return true;
		}
	}
}
