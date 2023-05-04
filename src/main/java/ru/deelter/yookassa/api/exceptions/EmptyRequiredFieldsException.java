package ru.deelter.yookassa.api.exceptions;

public class EmptyRequiredFieldsException extends RuntimeException {

	public EmptyRequiredFieldsException() {
		super("Some required parameters are not specified");
	}
}
