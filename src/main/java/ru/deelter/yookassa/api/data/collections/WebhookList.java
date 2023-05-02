package ru.deelter.yookassa.api.data.collections;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.Webhook;

import java.util.Collection;
import java.util.UUID;

public class WebhookList extends AbstractYooCollection<Webhook> {

	public WebhookList(@NotNull String type, @NotNull Collection<Webhook> items, @NotNull UUID nextCursor) {
		super(type, items, nextCursor);
	}
}