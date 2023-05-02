package ru.deelter.yookassa.api.data;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.UUID;

/*
{
  "id": "23d93cac-000f-5000-8000-126628f15141",
  "status": "pending",
  "paid": false,
  "amount": {
    "value": "100.00",
    "currency": "RUB"
  },
  "confirmation": {
    "type": "redirect",
    "confirmation_url": "https://yoomoney.ru/api-pages/v2/payment-confirm/epl?orderId=23d93cac-000f-5000-8000-126628f15141"
  },
  "created_at": "2019-01-22T14:30:45.129Z",
  "description": "Заказ №1",
  "metadata": {},
  "recipient": {
    "account_id": "100500",
    "gateway_id": "100700"
  },
  "refundable": false,
  "test": false
}
 */


public class Payment implements IYooObject {

	private final UUID id;
	private final PaymentStatus status;
	private final boolean paid;
	private final Amount amount;
	private final ConfirmationType confirmation;
	@SerializedName("created_at")
	private final Date createdAt;
	private final String description;
	private final JsonElement metadata;
	private final RecipientType recipient;
	private final boolean refundable;
	private final boolean test;
	private final String redirectUrl;

	public Payment(UUID id, PaymentStatus status, boolean paid, Amount amount, ConfirmationType confirmation, Date createdAt, String description, JsonElement metadata, RecipientType recipient, boolean refundable, boolean test, String redirectUrl) {
		this.id = id;
		this.status = status;
		this.paid = paid;
		this.amount = amount;
		this.confirmation = confirmation;
		this.createdAt = createdAt;
		this.description = description;
		this.metadata = metadata;
		this.recipient = recipient;
		this.refundable = refundable;
		this.test = test;
		this.redirectUrl = redirectUrl;
	}

	public UUID getId() {
		return id;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public boolean isPaid() {
		return paid;
	}

	public Amount getAmount() {
		return amount;
	}

	public ConfirmationType getConfirmation() {
		return confirmation;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public String getDescription() {
		return description;
	}

	public JsonElement getMetadata() {
		return metadata;
	}

	public RecipientType getRecipient() {
		return recipient;
	}

	public boolean isRefundable() {
		return refundable;
	}

	public boolean isTest() {
		return test;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	@Override
	public String toString() {
		return "Payment{" +
				"id=" + id +
				", status=" + status +
				", paid=" + paid +
				", amount=" + amount +
				", confirmation=" + confirmation +
				", createdAt=" + createdAt +
				", description='" + description + '\'' +
				", metadata=" + metadata +
				", recipient=" + recipient +
				", refundable=" + refundable +
				", test=" + test +
				", redirectUrl='" + redirectUrl + '\'' +
				'}';
	}

	public static class ConfirmationType {

		@SerializedName("confirmation_url")
		private final String url;
		private String type = "redirect";

		public ConfirmationType(@NotNull String type, @NotNull String confirmUrl) {
			this.type = type;
			this.url = confirmUrl;
		}

		public String getType() {
			return type;
		}

		public String getUrl() {
			return url;
		}

		@Override
		public String toString() {
			return "ConfirmationType{" +
					"type='" + type + '\'' +
					", url='" + url + '\'' +
					'}';
		}
	}
}