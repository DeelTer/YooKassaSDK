package ru.deelter.yookassa.api.data.requests.webhooks;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.YooUrls;

import java.util.UUID;

public class WebhookDeleteRequest extends YooRequest {

	public WebhookDeleteRequest(@NotNull UUID paymentId) {
		super(String.format(YooUrls.WEBHOOK_DELETE, paymentId), RequestMethod.DELETE);
	}
}
