package ru.deelter.yookassa.data.impl;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.data.IYooObject;
import ru.deelter.yookassa.exceptions.EmptyRequiredFieldsException;

/*
		{
			"description": "Чашка ебливая",
			"quantity": "2",
			"amount": {
				"value": "150.00",
				"currency": "RUB"
			},
			"vat_code": "1",
			"payment_mode": "full_payment",
      		"payment_subject": "commodity"
		}
 */

/**
 * <a href="https://yookassa.ru/developers/payment-acceptance/scenario-extensions/receipts/54fz/parameters-values">Parameters documentation</a>
 */
public class ReceiptItem implements IYooObject {

	private final String description;
	private final Amount amount;
	private final int quantity;
	@SerializedName("vat_code")
	private final int vatCode;
	@SerializedName("payment_mode")
	private final String paymentMode;
	@SerializedName("payment_subject")
	private final String paymentSubject;

	@Contract(pure = true)
	private ReceiptItem(@NotNull Builder builder) {
		description = builder.description;
		amount = builder.amount;
		quantity = builder.quantity;
		vatCode = builder.vatCode;
		paymentMode = builder.paymentMode;
		paymentSubject = builder.paymentSubject;
	}

	@Contract(value = " -> new", pure = true)
	public static @NotNull Builder builder() {
		return new Builder();
	}

	public static @NotNull Builder builder(@NotNull ReceiptItem copy) {
		Builder builder = new Builder();
		builder.description = copy.getDescription();
		builder.amount = copy.getAmount();
		builder.quantity = copy.getQuantity();
		builder.vatCode = copy.getVatCode();
		builder.paymentSubject = copy.paymentSubject;
		builder.paymentMode = copy.paymentMode;
		return builder;
	}

	public String getDescription() {
		return description;
	}

	public Amount getAmount() {
		return amount;
	}

	public int getQuantity() {
		return quantity;
	}

	public int getVatCode() {
		return vatCode;
	}

	public static final class Builder {
		private String description;
		private Amount amount;
		private int quantity = 1;
		private int vatCode = 1;
		private String paymentMode;
		private String paymentSubject;

		private Builder() {
		}

		public Builder paymentMode(PaymentMode paymentMode) {
			this.paymentMode = paymentMode.name().toLowerCase();
			return this;
		}

		public Builder paymentSubject(PaymentSubject paymentSubject) {
			this.paymentSubject = paymentSubject.name().toLowerCase();
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder amount(Amount amount) {
			this.amount = amount;
			return this;
		}

		public Builder quantity(int quantity) {
			this.quantity = quantity;
			return this;
		}

		/**
		 * <a href="https://yookassa.ru/developers/payment-acceptance/scenario-extensions/receipts/54fz/parameters-values">Parameters documentation</a>
		 */
		public Builder vatCode(int vatCode) {
			this.vatCode = vatCode;
			return this;
		}

		private boolean isValid() {
			return description != null && amount != null && quantity >= 1 && vatCode >= 1 && paymentMode != null && paymentSubject != null;
		}

		@Contract(value = " -> new", pure = true)
		public @NotNull ReceiptItem build() {
			if (!isValid())
				throw new EmptyRequiredFieldsException();
			return new ReceiptItem(this);
		}
	}
}
