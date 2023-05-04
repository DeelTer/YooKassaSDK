package ru.deelter.yookassa.requests.webhooks;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

import java.util.UUID;

public class WebhookDeleteRequest extends YooRequest {

	public WebhookDeleteRequest(@NotNull UUID paymentId) {
		super(String.format(YooRequestUrls.WEBHOOK_DELETE, paymentId), RequestMethod.DELETE);
	}
}
