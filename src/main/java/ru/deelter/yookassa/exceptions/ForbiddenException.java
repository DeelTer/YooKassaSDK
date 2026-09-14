package ru.deelter.yookassa.exceptions;

/**
 * HTTP 403: the credentials are not allowed to perform this operation.
 */
public class ForbiddenException extends YooKassaApiException {
	public ForbiddenException(int statusCode, String body) {
		super(statusCode, body);
	}
}
