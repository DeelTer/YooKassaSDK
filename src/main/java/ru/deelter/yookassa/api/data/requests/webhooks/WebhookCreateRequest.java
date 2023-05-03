package ru.deelter.yookassa.api.data.requests.webhooks;

import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.requests.WebhookCreateData;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.YooUrls;

public class WebhookCreateRequest extends YooRequest {

	public WebhookCreateRequest(@NotNull WebhookCreateData data) {
		super(YooUrls.WEBHOOK_CREATE, RequestMethod.POST, data);
	}
}
