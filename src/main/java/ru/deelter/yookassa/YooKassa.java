package ru.deelter.yookassa;

import okhttp3.*;
import ru.deelter.yookassa.exceptions.YooKassaApiException;
import ru.deelter.yookassa.model.JsonModel;
import ru.deelter.yookassa.utils.JsonUtil;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Synchronous, thread-safe YooKassa API v3 client. Create one instance per credential set and reuse it.
 * All API operations are inherited from {@link YooKassaOperations}.
 *
 * <p>Methods perform blocking I/O. They throw {@link YooKassaApiException} (unchecked) for non-2xx responses
 * and {@link IOException} for transport failures or unreadable 2xx responses.
 */
public final class YooKassa extends YooKassaOperations implements AutoCloseable {

	public static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");
	public static final String DEFAULT_BASE_URL = "https://api.yookassa.ru/v3";
	public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
	public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(40);
	public static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(60);

	private static final byte[] EMPTY_OBJECT = "{}".getBytes(StandardCharsets.UTF_8);

	private final OkHttpClient client;
	private final String authorization;
	private final HttpUrl baseUrl;
	private final RetryPolicy retryPolicy;
	private final boolean ownsClient;
	private volatile boolean closed;

	private YooKassa(Builder builder) {
		if (builder.authorization == null)
			throw new IllegalStateException("Credentials are required: call shop(...) or oauth(...)");
		authorization = builder.authorization;
		baseUrl = builder.baseUrl;
		retryPolicy = builder.retryPolicy;
		ownsClient = builder.httpClient == null;
		client = configure(ownsClient ? new OkHttpClient() : builder.httpClient, builder, ownsClient);
	}

	/**
	 * Shop ID and secret key authentication with default settings.
	 */
	public static YooKassa create(String shopId, String secretKey) {
		return builder().shop(shopId, secretKey).build();
	}

	/**
	 * OAuth token authentication with default settings; required for API-managed webhooks.
	 */
	public static YooKassa oauth(String token) {
		return builder().oauth(token).build();
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A new random idempotence key. Persist it with your operation before the first attempt.
	 */
	public static String newIdempotenceKey() {
		return UUID.randomUUID().toString();
	}

	private static OkHttpClient configure(OkHttpClient base, Builder builder, boolean defaults) {
		if (!defaults && builder.connectTimeout == null && builder.readTimeout == null && builder.callTimeout == null)
			return base;
		// newBuilder() shares the connection pool and dispatcher with the base client.
		return base.newBuilder()
			.connectTimeout(millis(builder.connectTimeout, DEFAULT_CONNECT_TIMEOUT, defaults, base.connectTimeoutMillis()), TimeUnit.MILLISECONDS)
			.readTimeout(millis(builder.readTimeout, DEFAULT_READ_TIMEOUT, defaults, base.readTimeoutMillis()), TimeUnit.MILLISECONDS)
			.callTimeout(millis(builder.callTimeout, DEFAULT_CALL_TIMEOUT, defaults, base.callTimeoutMillis()), TimeUnit.MILLISECONDS)
			.build();
	}

	private static long millis(Duration value, Duration fallback, boolean defaults, long current) {
		if (value != null) return value.toMillis();
		return defaults ? fallback.toMillis() : current;
	}

	/**
	 * Releases SDK-owned connections and threads and rejects further calls. Call after requests finish.
	 * An injected OkHttp client is never shut down.
	 */
	@Override
	public void close() {
		closed = true;
		if (ownsClient) {
			client.dispatcher().cancelAll();
			client.connectionPool().evictAll();
			client.dispatcher().executorService().shutdown();
		}
	}

	public HttpUrl getBaseUrl() {
		return baseUrl;
	}

	public RetryPolicy getRetryPolicy() {
		return retryPolicy;
	}

	/**
	 * The effective HTTP client, including SDK timeouts.
	 */
	OkHttpClient httpClient() {
		return client;
	}

	@Override
	<T> T execute(HttpMethod method, String[] path, JsonModel query, JsonModel body, String key, Class<T> responseType)
		throws IOException {
		if (closed) throw new IllegalStateException("YooKassa client is closed");
		HttpUrl url = RequestEncoding.url(baseUrl, path, query);
		byte[] payload = body != null ? body.toJson().getBytes(StandardCharsets.UTF_8) : method == HttpMethod.POST ? EMPTY_OBJECT : null;
		Request.Builder builder = new Request.Builder()
			.url(url)
			.header("Authorization", authorization)
			.header("Accept", "application/json")
			.method(method.name(), payload == null ? null : RequestBody.create(payload, MEDIA_TYPE_JSON));
		if (method != HttpMethod.GET) builder.header("Idempotence-Key", RequestEncoding.key(key));
		Request request = builder.build();

		// One serialized payload and one idempotence key for every attempt.
		for (int attempt = 1; ; attempt++) {
			if (closed) throw new IllegalStateException("YooKassa client is closed");
			Long retryAfter = null;
			try {
				return send(request, responseType);
			} catch (YooKassaApiException e) {
				if (!e.isRetryable() || !retryPolicy.canRetry(attempt)) throw e;
				retryAfter = e.getRetryAfterMillis();
			} catch (IOException e) {
				if (e instanceof ResponseParseException || closed || Thread.currentThread().isInterrupted()
					|| !retryPolicy.canRetry(attempt))
					throw e;
			}
			try {
				retryPolicy.sleep(retryPolicy.delayMillis(attempt, retryAfter));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedIOException("Interrupted while waiting to retry");
			}
		}
	}

	private <T> T send(Request request, Class<T> responseType) throws IOException {
		try (Response response = client.newCall(request).execute()) {
			ResponseBody responseBody = response.body();
			if (!response.isSuccessful()) {
				throw YooKassaApiException.of(response.code(), responseBody == null ? "" : responseBody.string());
			}
			if (responseType == null) return null;
			if (responseBody == null) throw new ResponseParseException("Missing API response body", null);
			try {
				T result = JsonUtil.fromJson(responseBody.string(), responseType);
				if (result == null) throw new ResponseParseException("Empty API response body", null);
				return result;
			} catch (com.google.gson.JsonParseException | IllegalStateException e) {
				throw new ResponseParseException("Invalid API response JSON", e);
			}
		}
	}

	/**
	 * A 2xx response whose body could not be read as the expected object; never retried.
	 */
	public static final class ResponseParseException extends IOException {
		ResponseParseException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * Client configuration. Exactly one of {@link #shop(String, String)} or {@link #oauth(String)} is required.
	 */
	public static final class Builder {
		private String authorization;
		private HttpUrl baseUrl = HttpUrl.get(DEFAULT_BASE_URL);
		private OkHttpClient httpClient;
		private Duration connectTimeout;
		private Duration readTimeout;
		private Duration callTimeout;
		private RetryPolicy retryPolicy = RetryPolicy.none();

		private Builder() {
		}

		/**
		 * Basic authentication with a shop ID and secret key from the merchant dashboard.
		 */
		public Builder shop(String shopId, String secretKey) {
			String id = credential(shopId, "shopId");
			if (id.indexOf(':') >= 0) throw new IllegalArgumentException("shopId must not contain ':'");
			String secret = credential(secretKey, "secretKey");
			authorization = "Basic " + Base64.getEncoder().encodeToString((id + ":" + secret).getBytes(StandardCharsets.UTF_8));
			return this;
		}

		public Builder shop(long shopId, String secretKey) {
			if (shopId <= 0) throw new IllegalArgumentException("shopId must be positive");
			return shop(Long.toString(shopId), secretKey);
		}

		/**
		 * OAuth bearer token from the partner flow.
		 */
		public Builder oauth(String token) {
			String value = credential(token, "token");
			for (int i = 0; i < value.length(); i++) {
				if (value.charAt(i) > 0x7E)
					throw new IllegalArgumentException("token must contain ASCII characters only");
			}
			authorization = "Bearer " + value;
			return this;
		}

		/**
		 * API root, for example a proxy or a local mock server. Defaults to {@link #DEFAULT_BASE_URL}.
		 */
		public Builder baseUrl(String baseUrl) {
			this.baseUrl = HttpUrl.get(Objects.requireNonNull(baseUrl, "baseUrl"));
			return this;
		}

		/**
		 * Caller-owned OkHttp client; {@link YooKassa#close()} will not shut it down.
		 * Explicit timeouts are applied to a derived client sharing its pool and dispatcher.
		 */
		public Builder httpClient(OkHttpClient httpClient) {
			this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
			return this;
		}

		public Builder connectTimeout(Duration timeout) {
			connectTimeout = timeout(timeout);
			return this;
		}

		public Builder readTimeout(Duration timeout) {
			readTimeout = timeout(timeout);
			return this;
		}

		/**
		 * Limit for one attempt, including connection, request and response.
		 */
		public Builder callTimeout(Duration timeout) {
			callTimeout = timeout(timeout);
			return this;
		}

		public Builder retryPolicy(RetryPolicy retryPolicy) {
			this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
			return this;
		}

		public YooKassa build() {
			return new YooKassa(this);
		}

		private static Duration timeout(Duration timeout) {
			Objects.requireNonNull(timeout, "timeout");
			if (timeout.isNegative()) throw new IllegalArgumentException("timeout must not be negative");
			return timeout;
		}

		/**
		 * Rejects blanks and control characters that would corrupt the Authorization header.
		 */
		private static String credential(String value, String name) {
			Objects.requireNonNull(value, name);
			if (value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c < 0x20 || c == 0x7F) throw new IllegalArgumentException(name + " contains control characters");
			}
			return value;
		}
	}
}
