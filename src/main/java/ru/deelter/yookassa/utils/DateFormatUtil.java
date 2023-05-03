package ru.deelter.yookassa.utils;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatUtil {

	private static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

	public static @NotNull String formatISO(@NotNull Date date) {
		return ISO_DATE_FORMAT.format(date);
	}
}
