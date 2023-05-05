package ru.deelter.yookassa.data.impl.requests;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.IYooPayment;
import ru.deelter.yookassa.data.IYooRequestData;
import ru.deelter.yookassa.data.impl.Amount;

import java.util.UUID;

public class RefundCreateData implements IYooRequestData {

	@SerializedName("payment_id")
	private final UUID paymentId;
	private final Amount amount;

	public RefundCreateData(@NotNull UUID paymentId, @NotNull Amount amount) {
		this.amount = amount;
		this.paymentId = paymentId;
	}

	@Contract(value = "_, _ -> new", pure = true)
	public static @NotNull RefundCreateData from(@NotNull UUID paymentId, @NotNull Amount amount) {
		return new RefundCreateData(paymentId, amount);
	}

	@Contract("_, _ -> new")
	public static @NotNull RefundCreateData from(@NotNull IYooPayment payment, @NotNull Amount amount) {
		return from(payment.getId(), amount);
	}

	public Amount getAmount() {
		return amount;
	}

	public UUID getPaymentId() {
		return paymentId;
	}
}