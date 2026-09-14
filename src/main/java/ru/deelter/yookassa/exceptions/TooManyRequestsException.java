package ru.deelter.yookassa.exceptions;

/**
 * HTTP 429: rate limited. Retry later with the same idempotence key.
 */
public class TooManyRequestsException extends YooKassaApiException {
	public TooManyRequestsException(int statusCode, String body) {
		super(statusCode, body);
	}
}
