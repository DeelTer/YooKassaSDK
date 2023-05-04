package ru.deelter.yookassa.exceptions;

import org.jetbrains.annotations.NotNull;

public abstract class RequestBuilderException extends RuntimeException {

	public RequestBuilderException() {
		super();
	}

	public RequestBuilderException(@NotNull String reason) {
		super(reason);
	}
}
