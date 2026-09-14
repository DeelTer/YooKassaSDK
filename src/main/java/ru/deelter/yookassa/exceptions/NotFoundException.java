package ru.deelter.yookassa.exceptions;

/**
 * HTTP 404: the object does not exist or belongs to another shop.
 */
public class NotFoundException extends YooKassaApiException {
	public NotFoundException(int statusCode, String body) {
		super(statusCode, body);
	}
}
