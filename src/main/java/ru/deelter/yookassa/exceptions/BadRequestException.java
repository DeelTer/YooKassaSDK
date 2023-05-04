package ru.deelter.yookassa.exceptions;

public class BadRequestException extends RuntimeException {

	public BadRequestException(String response) {
		super(response);
	}

}
