package ru.deelter.yookassa.exceptions;

import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class BadRequestException extends RuntimeException {

	public BadRequestException(String response) {
		super(response);
	}

	public BadRequestException(@NotNull Response response) {
		super(String.format("Code: %s Message: %s", response.code(), response.message()));
	}
}
