package ru.deelter.yookassa.data.impl.collections;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.Refund;

import java.util.Collection;
import java.util.UUID;

public class RefundList extends AbstractYooCollection<Refund> {

	public RefundList(@NotNull String type, @NotNull Collection<Refund> items, @NotNull UUID nextCursor) {
		super(type, items, nextCursor);
	}
}