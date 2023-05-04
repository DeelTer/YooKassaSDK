package ru.deelter.yookassa.utils;

import org.jetbrains.annotations.NotNull;

public class CustomerUtil {

	private static final String PHONE_REGEX = "^(7|8)(?:(-| )?\\d{3}){2}(?:(-| )?\\d{2}){2}$";
	private static final String EMAIL_REGEX = "[^@\\s]+@[^@\\s]+\\.[^@\\s]+";

	public static boolean isValidPhone(@NotNull String number) {
		return number.matches(PHONE_REGEX);
	}

	public static @NotNull String formatPhone(@NotNull String number) {
		return number.replace("-", "")
				.replace(" ", "")
				.replace("+", "")
				.trim();
	}

	public static boolean isValidEmail(@NotNull String email) {
		return email.matches(EMAIL_REGEX);
	}
}
