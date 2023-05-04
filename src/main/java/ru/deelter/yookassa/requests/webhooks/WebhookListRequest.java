package ru.deelter.yookassa.requests.webhooks;

import ru.deelter.yookassa.data.impl.PaymentStatus;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.utils.YooRequestUrls;

public class WebhookListRequest extends YooRequest {

	private int limit;
	private PaymentStatus status;

	public WebhookListRequest() {
		super(YooRequestUrls.WEBHOOK_GET_LIST, RequestMethod.GET);
	}

	private WebhookListRequest(Builder builder) {
		this();
		limit = builder.limit;
		status = builder.status;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getUrlParameters() {
		String params = "?limit=" + limit + "&";
		if (status != null)
			params += "status=" + status;
		return params;
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
