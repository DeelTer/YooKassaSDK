package ru.deelter.yookassa.exceptions;

/**
 * HTTP 400: malformed request or invalid parameters. Do not retry unchanged.
 */
public class InvalidRequestException extends YooKassaApiException {
	public InvalidRequestException(int statusCode, String body) {
		super(statusCode, body);
	}
}
