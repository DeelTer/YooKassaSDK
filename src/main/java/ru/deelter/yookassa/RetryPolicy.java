package ru.deelter.yookassa;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Automatic repetition of one API call with the same payload and the same idempotence key.
 * Transport failures, HTTP 429 and HTTP 5xx are retried; the server's {@code retry_after} hint wins
 * over exponential backoff when it is longer. Immutable and thread-safe.
 *
 * <p>Retries block the calling thread. A retried POST is safe only because YooKassa deduplicates
 * requests by idempotence key for 24 hours; after the last attempt the outcome may still be unknown.
 */
public final class RetryPolicy {

	/**
	 * Sleeps between attempts; replaceable in tests.
	 */
	public interface Sleeper {
		void sleep(long millis) throws InterruptedException;
	}

	private static final RetryPolicy NONE = new RetryPolicy(builder().maxAttempts(1));

	private final int maxAttempts;
	private final long initialDelayMillis;
	private final long maxDelayMillis;
	private final boolean jitter;
	private final Sleeper sleeper;

	private RetryPolicy(Builder builder) {
		maxAttempts = builder.maxAttempts;
		initialDelayMillis = builder.initialDelayMillis;
		maxDelayMillis = builder.maxDelayMillis;
		jitter = builder.jitter;
		sleeper = builder.sleeper;
	}

	/**
	 * One attempt, no retries. This is the client default.
	 */
	public static RetryPolicy none() {
		return NONE;
	}

	/**
	 * Three attempts, backoff from 500 ms up to 10 s with jitter.
	 */
	public static RetryPolicy defaults() {
		return builder().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	boolean canRetry(int failedAttempts) {
		return failedAttempts < maxAttempts;
	}

	/**
	 * Delay after the given number of failed attempts (1-based).
	 */
	long delayMillis(int failedAttempts, Long retryAfterMillis) {
		long backoff = initialDelayMillis << Math.min(failedAttempts - 1, 20);
		backoff = Math.min(backoff, maxDelayMillis);
		if (jitter && backoff > 1) backoff = backoff / 2 + ThreadLocalRandom.current().nextLong(backoff / 2 + 1);
		if (retryAfterMillis != null) backoff = Math.max(backoff, Math.min(retryAfterMillis, maxDelayMillis));
		return backoff;
	}

	void sleep(long millis) throws InterruptedException {
		if (millis > 0) sleeper.sleep(millis);
	}

	public static final class Builder {
		private int maxAttempts = 3;
		private long initialDelayMillis = 500;
		private long maxDelayMillis = 10_000;
		private boolean jitter = true;
		private Sleeper sleeper = Thread::sleep;

		private Builder() {
		}

		/**
		 * Total attempts including the first one; 1 disables retries.
		 */
		public Builder maxAttempts(int maxAttempts) {
			if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be at least 1");
			this.maxAttempts = maxAttempts;
			return this;
		}

		public Builder initialDelay(Duration delay) {
			initialDelayMillis = positive(delay, "initialDelay");
			return this;
		}

		/**
		 * Upper bound for backoff and for the server's retry_after hint.
		 */
		public Builder maxDelay(Duration delay) {
			maxDelayMillis = positive(delay, "maxDelay");
			return this;
		}

		public Builder jitter(boolean jitter) {
			this.jitter = jitter;
			return this;
		}

		public Builder sleeper(Sleeper sleeper) {
			this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
			return this;
		}

		public RetryPolicy build() {
			if (initialDelayMillis > maxDelayMillis)
				throw new IllegalArgumentException("initialDelay exceeds maxDelay");
			return new RetryPolicy(this);
		}

		private static long positive(Duration delay, String name) {
			Objects.requireNonNull(delay, name);
			if (delay.isNegative()) throw new IllegalArgumentException(name + " must not be negative");
			return delay.toMillis();
		}
	}
}
