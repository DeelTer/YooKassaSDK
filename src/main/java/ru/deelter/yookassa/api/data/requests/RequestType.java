package ru.deelter.yookassa.api.data.requests;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.exceptions.BadRequestException;

public enum RequestType {

	PAYMENT_CREATE("https://api.yookassa.ru/v3/payments", RequestMethod.POST),
	PAYMENT_GET("https://api.yookassa.ru/v3/payments/%s", RequestMethod.GET, true),
	PAYMENT_GET_ALL("https://api.yookassa.ru/v3/payments", RequestMethod.GET),
	REFUND_CREATE("https://api.yookassa.ru/v3/refunds", RequestMethod.POST),
	REFUND_GET("https://api.yookassa.ru/v3/refunds/%s", RequestMethod.GET, true),
	REFUNDS_GET("https://api.yookassa.ru/v3/refunds", RequestMethod.GET),
	WEBHOOK_CREATE("https://api.yookassa.ru/v3/webhooks", RequestMethod.POST),
	WEBHOOK_DELETE("https://api.yookassa.ru/v3/webhooks/%s", RequestMethod.DELETE, true),
	WEBHOOK_GET_ALL("https://api.yookassa.ru/v3/webhooks", RequestMethod.GET);

	private final String url;
	private final RequestMethod method;
	private final boolean requireObjects;

	RequestType(@NotNull String url, @NotNull RequestMethod method, boolean requireObjects) {
		this.url = url;
		this.method = method;
		this.requireObjects = requireObjects;
	}

	RequestType(@NotNull String url, @NotNull RequestMethod method) {
		this(url, method, false);
	}

	@Contract("_ -> new")
	public @NotNull YooRequest createRequest(@NotNull Object... objects) throws BadRequestException {
		String url;
		if (requireObjects) {
			if (objects == null || objects.length == 0)
				throw new BadRequestException("Request require some objects");
			url = String.format(this.url, objects);
		} else
			url = this.url;
		return new YooRequest(url, this);
	}

	public RequestMethod getMethod() {
		return method;
	}


}
