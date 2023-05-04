package ru.deelter.yookassa.data.impl;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.IYooPayment;

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

public class Payment implements IYooPayment {

	private UUID id;
	private PaymentStatus status;
	private boolean paid;
	private Amount amount;
	private ConfirmationType confirmation;
	@SerializedName("created_at")
	private Date createdAt;
	@SerializedName("expires_at")
	private Date expiresAt;
	private String description;
	private JsonElement metadata;
	private RecipientType recipient;
	private Receipt receipt;
	private boolean refundable;
	private boolean test;
	private String redirectUrl;

	@NotNull
	@Override
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

	public Receipt getReceipt() {
		return receipt;
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

	public Date getExpiresAt() {
		return expiresAt;
	}

	public static class ConfirmationType {

		@SerializedName("confirmation_url")
		private final String url;
		private String type = "redirect";

		public ConfirmationType(@NotNull String type, @NotNull String confirmUrl) {
			this.type = type;
			this.url = confirmUrl;
		}

		public ConfirmationType(@NotNull String confirmUrl) {
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