package ru.deelter.yookassa.exceptions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Non-2xx YooKassa HTTP response. Catch a status-specific subclass or this base type.
 * The exception retains parsed data and the raw body, never an open HTTP response.
 */
public class YooKassaApiException extends RuntimeException {
	private final int statusCode;
	private final String responseBody;
	private String code;
	private String description;
	private String parameter;
	private String requestId;
	private Long retryAfterMillis;

	public YooKassaApiException(int statusCode, String body) {
		// Avoid putting potentially sensitive server-provided data in log messages.
		super("YooKassa HTTP " + statusCode);
		this.statusCode = statusCode;
		responseBody = body;
		if (body != null && !body.isEmpty()) {
			try {
				JsonObject error = JsonParser.parseString(body).getAsJsonObject();
				code = string(error, "code");
				description = string(error, "description");
				parameter = string(error, "parameter");
				requestId = string(error, "id");
				JsonElement retry = error.get("retry_after");
				if (retry != null && retry.isJsonPrimitive() && retry.getAsJsonPrimitive().isNumber())
					retryAfterMillis = Math.max(0, retry.getAsLong());
			} catch (RuntimeException ignored) {
				// A proxy may return HTML or an empty body instead of an API error.
			}
		}
	}

	/**
	 * Creates the most specific exception for an HTTP status.
	 */
	public static YooKassaApiException of(int statusCode, String body) {
		switch (statusCode) {
			case 400:
				return new InvalidRequestException(statusCode, body);
			case 401:
				return new AuthenticationException(statusCode, body);
			case 403:
				return new ForbiddenException(statusCode, body);
			case 404:
				return new NotFoundException(statusCode, body);
			case 429:
				return new TooManyRequestsException(statusCode, body);
			default:
				return statusCode >= 500 ? new ServerErrorException(statusCode, body) : new YooKassaApiException(statusCode, body);
		}
	}

	private static String string(JsonObject object, String name) {
		JsonElement value = object.get(name);
		return value == null || !value.isJsonPrimitive() ? null : value.getAsString();
	}

	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Raw server data; redact customer information before logging.
	 */
	public String getResponseBody() {
		return responseBody;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public String getParameter() {
		return parameter;
	}

	public String getRequestId() {
		return requestId;
	}

	/**
	 * Server-recommended delay before a retry, in milliseconds; null when absent.
	 */
	public Long getRetryAfterMillis() {
		return retryAfterMillis;
	}

	/**
	 * True for statuses where repeating the same request with the same idempotence key is reasonable:
	 * 429 and 5xx. The outcome of a 5xx operation is unknown until its status is reconciled.
	 */
	public boolean isRetryable() {
		return statusCode == 429 || statusCode >= 500;
	}
}
