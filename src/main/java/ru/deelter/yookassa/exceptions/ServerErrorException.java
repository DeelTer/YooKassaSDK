package ru.deelter.yookassa.exceptions;

/**
 * HTTP 5xx: the outcome is unknown. Retry with the same key or reconcile the object status.
 */
public class ServerErrorException extends YooKassaApiException {
	public ServerErrorException(int statusCode, String body) {
		super(statusCode, body);
	}
}
