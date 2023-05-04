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
			"vat_code": "1"
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

	@Contract(pure = true)
	private ReceiptItem(@NotNull Builder builder) {
		description = builder.description;
		amount = builder.amount;
		quantity = builder.quantity;
		vatCode = builder.vatCode;
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

		private Builder() {
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
			return description != null && amount != null && quantity >= 1 && vatCode >= 1;
		}

		@Contract(value = " -> new", pure = true)
		public @NotNull ReceiptItem build() {
			if (!isValid())
				throw new EmptyRequiredFieldsException();
			return new ReceiptItem(this);
		}
	}
}
