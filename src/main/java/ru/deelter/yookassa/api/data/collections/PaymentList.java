package ru.deelter.yookassa.api.data.collections;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.Payment;

import java.util.Collection;
import java.util.UUID;

public class PaymentList extends AbstractYooCollection<Payment> {

	public PaymentList(@NotNull String type, @NotNull Collection<Payment> items, @NotNull UUID nextCursor) {
		super(type, items, nextCursor);
	}
}