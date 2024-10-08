package ru.deelter.yookassa.data.impl.requests;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.yookassa.data.IYooRequestData;
import ru.deelter.yookassa.data.impl.Amount;
import ru.deelter.yookassa.data.impl.Receipt;
import ru.deelter.yookassa.exceptions.EmptyBuilderException;

public class PaymentCreateData implements IYooRequestData {

	private final Amount amount;
	private final ConfirmationType confirmation;
	private final String description;
	@SerializedName("metadata")
	private final JsonElement metadata;
	private final Receipt receipt;
	private final boolean capture;

	public PaymentCreateData(@NotNull Amount amount, @NotNull ConfirmationType confirmation, @NotNull String description, Receipt receipt, JsonElement metadata, boolean capture) {
		this.amount = amount;
		this.confirmation = confirmation;
		this.receipt = receipt;
		this.capture = capture;
		this.description = description;
		this.metadata = metadata;
	}

	@Contract(pure = true)
	private PaymentCreateData(@NotNull Builder builder) {
		amount = builder.amount;
		confirmation = builder.confirmation;
		description = builder.description;
		metadata = builder.metadata;
		capture = builder.capture;
		receipt = builder.receipt;
	}

	public static @NotNull PaymentCreateData create(@NotNull Amount amount, @NotNull String redirectUrl, @NotNull String description, @Nullable JsonElement metaData, @Nullable Receipt receipt) {
		return new PaymentCreateData(amount, new ConfirmationType(redirectUrl), description, receipt, metaData, true);
	}

	public static @NotNull PaymentCreateData create(@NotNull Amount amount, @NotNull String redirectUrl, @NotNull String description, @Nullable Receipt receipt) {
		return create(amount, redirectUrl, description, null, receipt);
	}

	public static @NotNull PaymentCreateData create(@NotNull Amount amount, @NotNull String redirectUrl, @NotNull String description) {
		return create(amount, redirectUrl, description, null, null);
	}

	@Contract(value = " -> new", pure = true)
	public static @NotNull Builder builder() {
		return new Builder();
	}

	public static @NotNull Builder builder(@NotNull PaymentCreateData copy) {
		Builder builder = new Builder();
		builder.amount = copy.getAmount();
		builder.confirmation = copy.getConfirmation();
		builder.description = copy.getDescription();
		builder.metadata = copy.getMetadata();
		builder.capture = copy.isCapture();
		builder.receipt = copy.getReceipt();
		return builder;
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

	public JsonElement getMetadata() {
		return metadata;
	}

	public boolean isCapture() {
		return capture;
	}

	public Receipt getReceipt() {
		return receipt;
	}

	public static class ConfirmationType {

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
		private JsonElement metadata;
		private Receipt receipt;
		private boolean capture;

		private Builder() {
		}

		@NotNull
		public Builder amount(@NotNull Amount amount) {
			this.amount = amount;
			return this;
		}

		@NotNull
		public Builder confirmation(@NotNull PaymentCreateData.ConfirmationType confirmation) {
			this.confirmation = confirmation;
			return this;
		}

		public Builder redirect(@NotNull String redirectUrl) {
			this.confirmation = new ConfirmationType(redirectUrl);
			return this;
		}

		@NotNull
		public Builder description(@NotNull String description) {
			this.description = description;
			return this;
		}

		@NotNull
		public Builder metadata(@NotNull JsonElement metadata) {
			this.metadata = metadata;
			return this;
		}

		@NotNull
		public Builder capture(boolean capture) {
			this.capture = capture;
			return this;
		}

		@NotNull
		public Builder receipt(@NotNull Receipt receipt) {
			this.receipt = receipt;
			return this;
		}

		private boolean isEmpty() {
			return amount == null && confirmation == null && description == null && metadata == null;
		}

		@NotNull
		public PaymentCreateData build() {
			if (isEmpty())
				throw new EmptyBuilderException();
			return new PaymentCreateData(this);
		}
	}
}
