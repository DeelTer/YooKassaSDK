package ru.deelter.yookassa.api.data.requests;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.data.Amount;

public class PaymentRequest implements IYooRequestData {

	private final Amount amount;
	private final ConfirmationType confirmation;
	private final String description;
	private final boolean capture;

	public PaymentRequest(@NotNull Amount amount, @NotNull ConfirmationType confirmation, @NotNull String description, boolean capture) {
		this.amount = amount;
		this.confirmation = confirmation;
		this.capture = capture;
		this.description = description;
	}

	@Contract(pure = true)
	private PaymentRequest(@NotNull Builder builder) {
		amount = builder.amount;
		confirmation = builder.confirmation;
		description = builder.description;
		capture = builder.capture;
	}

	@Contract(value = " -> new", pure = true)
	public static @NotNull Builder builder() {
		return new Builder();
	}

	public static @NotNull Builder builder(@NotNull PaymentRequest copy) {
		Builder builder = new Builder();
		builder.amount = copy.getAmount();
		builder.confirmation = copy.getConfirmation();
		builder.description = copy.getDescription();
		builder.capture = copy.isCapture();
		return builder;
	}

	@Contract("_, _, _ -> new")
	public static @NotNull PaymentRequest create(@NotNull Amount amount, @NotNull String redirectUrl, @NotNull String description) {
		return new PaymentRequest(amount, new ConfirmationType(redirectUrl), description, true);
	}

	public Amount getAmount() {
		return amount;
	}

	public ConfirmationType getConfirmation() {
		return confirmation;
	}

	public String getDescription() {
		return description;
	}

	public boolean isCapture() {
		return capture;
	}

	private static class ConfirmationType {

		protected final String type;
		@SerializedName("return_url")
		protected final String returnUrl;

		public ConfirmationType(String type, String redirectUrl) {
			this.type = type;
			this.returnUrl = redirectUrl;
		}

		public ConfirmationType(String redirectUrl) {
			this("redirect", redirectUrl);
		}

		public String getType() {
			return type;
		}

		public String getReturnUrl() {
			return returnUrl;
		}
	}

	public static final class Builder {
		private Amount amount;
		private ConfirmationType confirmation;
		private String description;
		private boolean capture = true;

		private Builder() {
		}

		@NotNull
		public Builder amount(@NotNull Amount amount) {
			this.amount = amount;
			return this;
		}

		@NotNull
		public Builder confirmation(@NotNull PaymentRequest.ConfirmationType confirmation) {
			this.confirmation = confirmation;
			return this;
		}

		@NotNull
		public Builder redirect(@NotNull String url) {
			this.confirmation = new ConfirmationType(url);
			return this;
		}

		@NotNull
		public Builder description(@NotNull String description) {
			this.description = description;
			return this;
		}

		@NotNull
		public Builder capture(boolean capture) {
			this.capture = capture;
			return this;
		}

		@NotNull
		public PaymentRequest build() {
			return new PaymentRequest(this);
		}
	}
}
