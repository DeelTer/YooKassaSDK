package ru.deelter.yookassa.api.data.requests;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.Amount;

import java.util.UUID;

public class RefundCreateData implements IYooRequestData {

	private final Amount amount;
	@SerializedName("payment_id")
	private final UUID paymentId;

	public RefundCreateData(@NotNull Amount amount, @NotNull UUID paymentId) {
		this.amount = amount;
		this.paymentId = paymentId;
	}

	public Amount getAmount() {
		return amount;
	}

	public UUID getPaymentId() {
		return paymentId;
	}
}