package ru.deelter.yookassa.exceptions;

import org.jetbrains.annotations.NotNull;

public class InvalidInnLengthException extends RuntimeException {

	public InvalidInnLengthException(@NotNull String inn) {
		super(String.format("Required length: 12. Current value: %s", inn.length()));
	}
}
