package ru.deelter.yookassa.api.exceptions;

import org.jetbrains.annotations.Nullable;

public class InvalidPhoneException extends RuntimeException {

	public InvalidPhoneException(@Nullable String number) {
		super(String.format("Number %s is not valid", number));
	}
}
