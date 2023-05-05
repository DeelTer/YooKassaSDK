package ru.deelter.yookassa.data.impl;

/*
{
  "id": "216749f7-0016-50be-b000-078d43a63ae4",
  "status": "succeeded",
  "amount": {
    "value": "1",
    "currency": "RUB"
  },
  "created_at": "2017-10-04T19:27:51.407Z",
  "payment_id": "216749da-000f-50be-b000-096747fad91e"
}
 */

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.IYooRefund;

import java.util.Date;
import java.util.UUID;

public class Refund implements IYooRefund {

	private final UUID id;
	private final String status;
	private final Amount amount;
	@SerializedName("created_at")
	private final Date createdAt;
	@SerializedName("payment_id")
	private final UUID paymentId;

	public Refund(@NotNull UUID id, @NotNull String status, Amount amount, Date createdAt, UUID paymentId) {
		this.id = id;
		this.status = status;
		this.amount = amount;
		this.createdAt = createdAt;
		this.paymentId = paymentId;
	}

	@Override
	public @NotNull UUID getId() {
		return id;
	}

	public String getStatus() {
		return status;
	}

	public Amount getAmount() {
		return amount;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public UUID getPaymentId() {
		return paymentId;
	}
}