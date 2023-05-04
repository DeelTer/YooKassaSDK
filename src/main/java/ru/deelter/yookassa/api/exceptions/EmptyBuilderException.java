package ru.deelter.yookassa.api.exceptions;

public class EmptyBuilderException extends RuntimeException {

	public EmptyBuilderException() {
		super("Parameters are not specified");
	}
}
