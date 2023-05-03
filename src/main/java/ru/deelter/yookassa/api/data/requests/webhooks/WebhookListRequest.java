package ru.deelter.yookassa.api.data.requests.webhooks;

import ru.deelter.yookassa.api.data.PaymentStatus;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.YooUrls;

public class WebhookListRequest extends YooRequest {

	private int limit;
	private PaymentStatus status;

	public WebhookListRequest() {
		super(YooUrls.WEBHOOK_GET_LIST, RequestMethod.GET);
	}

	private WebhookListRequest(Builder builder) {
		this();
		limit = builder.limit;
		status = builder.status;
	}

	@Override
	public String getUrlParameters() {
		String params = "?limit=" + limit + "&";
		if (status != null)
			params += "status=" + status;
		return params;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private int limit = 10;
		private PaymentStatus status;

		private Builder() {
		}

		public Builder limit(int limit) {
			this.limit = limit;
			return this;
		}

		public Builder status(PaymentStatus status) {
			this.status = status;
			return this;
		}

		public WebhookListRequest build() {
			return new WebhookListRequest(this);
		}
	}
}
