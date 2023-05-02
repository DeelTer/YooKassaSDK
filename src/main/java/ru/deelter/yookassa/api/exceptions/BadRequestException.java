package ru.deelter.yookassa.api.exceptions;

public class BadRequestException extends RuntimeException {

	public BadRequestException(String response) {
		super(response);
	}

}
