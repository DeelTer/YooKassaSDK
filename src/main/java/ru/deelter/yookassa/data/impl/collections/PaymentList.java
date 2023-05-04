package ru.deelter.yookassa.data.impl.collections;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.Payment;

import java.util.Collection;
import java.util.UUID;

public class PaymentList extends AbstractYooCollection<Payment> {

	public PaymentList(@NotNull String type, @NotNull Collection<Payment> items, @NotNull UUID nextCursor) {
		super(type, items, nextCursor);
	}
}