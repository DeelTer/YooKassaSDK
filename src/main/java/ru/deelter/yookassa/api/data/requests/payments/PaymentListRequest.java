package ru.deelter.yookassa.api.data.requests.payments;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.PaymentStatus;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.YooUrls;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class PaymentListRequest extends YooRequest {

	private final int limit;
	private final PaymentStatus status;
	private final OffsetDateTime afterTime;
	private final OffsetDateTime beforeTime;

	public PaymentListRequest(int limit, PaymentStatus status, OffsetDateTime afterTime, OffsetDateTime beforeTime) {
		super(YooUrls.PAYMENT_GET_LIST, RequestMethod.GET);
		this.limit = limit;
		this.status = status;
		this.afterTime = afterTime;
		this.beforeTime = beforeTime;
	}

	private PaymentListRequest(@NotNull Builder builder) {
		this(builder.limit, builder.status, builder.afterTime, builder.beforeTime);
	}

	@Contract(value = " -> new", pure = true)
	public static @NotNull Builder builder() {
		return new Builder();
	}

	@Override
	public String getUrlParameters() {
		String params = "?limit=" + limit;
		if (status != null)
			params += "&status=" + status;
		if (beforeTime != null)
			params += "&created_at.lte=" + beforeTime.format(DateTimeFormatter.ISO_INSTANT);
		if (afterTime != null)
			params += "&created_at.gte=" + afterTime.format(DateTimeFormatter.ISO_INSTANT);
		return params;
	}

	public static final class Builder {
		private int limit;
		private PaymentStatus status;
		private OffsetDateTime afterTime;
		private OffsetDateTime beforeTime;

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

		public Builder after(OffsetDateTime afterDate) {
			this.afterTime = afterDate;
			return this;
		}

		public Builder before(OffsetDateTime beforeDate) {
			this.beforeTime = beforeDate;
			return this;
		}

		public PaymentListRequest build() {
			return new PaymentListRequest(this);
		}
	}
}
