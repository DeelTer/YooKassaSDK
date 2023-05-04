package ru.deelter.yookassa.exceptions;

public class EmptyBuilderException extends RuntimeException {

	public EmptyBuilderException() {
		super("Parameters are not specified");
	}
}
