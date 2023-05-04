package ru.deelter.yookassa.exceptions;

public class EmptyRequiredFieldsException extends RuntimeException {

	public EmptyRequiredFieldsException() {
		super("Some required parameters are not specified");
	}
}
