package ru.deelter.yookassa.api.events;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum YooKassaEvent {

	PAYMENT_WAITING_FOR_CAPTURE("payment.waiting_for_capture"),
	PAYMENT_SUCCESS_PAID("payment.succeeded"),
	PAYMENT_CANCELED("payment.canceled"),
	REFUND_SUCCESS("refund.succeeded");

	@SerializedName("eventName")
	private final String name;

	YooKassaEvent(@NotNull String name) {
		this.name = name;
	}

	public static @Nullable YooKassaEvent getByName(@NotNull String name) {
		for (YooKassaEvent event : values()) {
			if (event.getName().equalsIgnoreCase(name))
				return event;
		}
		return null;
	}

	public String getName() {
		return name;
	}
}
