package ru.deelter.yookassa.requests.webhooks;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.impl.requests.WebhookCreateData;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

public class WebhookCreateRequest extends YooRequest {

	public WebhookCreateRequest(@NotNull WebhookCreateData data) {
		super(YooRequestUrls.WEBHOOK_CREATE, RequestMethod.POST, data);
	}
}
