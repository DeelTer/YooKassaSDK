package ru.deelter.yookassa.exceptions;

/**
 * HTTP 401: invalid shop ID, secret key or OAuth token.
 */
public class AuthenticationException extends YooKassaApiException {
	public AuthenticationException(int statusCode, String body) {
		super(statusCode, body);
	}
}
