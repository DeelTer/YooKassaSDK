package ru.deelter.yookassa.api.data.requests.payments;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.PaymentStatus;
import ru.deelter.yookassa.api.data.requests.YooRequest;
import ru.deelter.yookassa.utils.DateFormatUtil;
import ru.deelter.yookassa.utils.YooUrls;

import java.util.Date;

public class PaymentListRequest extends YooRequest {

	private int limit;
	private PaymentStatus status;
	private final Date afterDate;
	private final Date beforeDate;

	public PaymentListRequest(int limit, PaymentStatus status, Date afterDate, Date beforeDate) {
		super(YooUrls.PAYMENT_GET_LIST, RequestMethod.GET);
		this.limit = limit;
		this.status = status;
		this.afterDate = afterDate;
		this.beforeDate = beforeDate;
	}

	private PaymentListRequest(@NotNull Builder builder) {
		this(builder.limit, builder.status, builder.afterDate, builder.beforeDate);
		limit = builder.limit;
		status = builder.status;
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
		if (beforeDate != null)
			params += "&created_at.lte=" + DateFormatUtil.formatISO(beforeDate);
		if (afterDate != null)
			params += "&created_at.gte=" + DateFormatUtil.formatISO(afterDate);
		return params;
	}

	public static final class Builder {
		private int limit;
		private PaymentStatus status;
		private Date afterDate;
		private Date beforeDate;

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

		public Builder afterDate(Date afterDate) {
			this.afterDate = afterDate;
			return this;
		}

		public Builder beforeDate(Date beforeDate) {
			this.beforeDate = beforeDate;
			return this;
		}

		public PaymentListRequest build() {
			return new PaymentListRequest(this);
		}
	}
}
