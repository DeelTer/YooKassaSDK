package ru.deelter.yookassa.api.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import ru.deelter.yookassa.api.exceptions.ReceiptItemLimitException;
import ru.deelter.yookassa.api.exceptions.ReceiptItemsEmptyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
{
	"customer": {
		"email": "user@example.com"
	},
	"items": [
		{
			"description": "Ложка хуёжка",
			"quantity": "10",
			"amount": {
				"value": "50.00",
				"currency": "RUB"
			},
			"vat_code": "1"
		},
		{
			"description": "Чашка ебаная",
			"quantity": "2",
			"amount": {
				"value": "150.00",
				"currency": "RUB"
			},
			"vat_code": "1"
		},
		{
			"description": "Блюдце еблюдце",
			"quantity": "3",
			"amount": {
				"value": "100.00",
				"currency": "RUB"
			},
			"vat_code": "1"
		}
	]
}
 */
public class Receipt implements IYooObject {

	private final Customer customer;
	private final List<ReceiptItem> items; // 6 max

	public Receipt(@NotNull Customer customer, @NotNull List<ReceiptItem> items) {
		this.customer = customer;
		this.items = items;
	}

	private Receipt(@NotNull Builder builder) {
		customer = builder.customer;
		items = builder.items;
	}

	@Contract(value = " -> new", pure = true)
	public static @NotNull Builder builder() {
		return new Builder();
	}

	public static @NotNull Builder builder(@NotNull Receipt copy) {
		Builder builder = new Builder();
		builder.customer = copy.getCustomer();
		builder.items = copy.getItems();
		return builder;
	}

	public Customer getCustomer() {
		return customer;
	}

	public List<ReceiptItem> getItems() {
		return items;
	}

	public static final class Builder {
		private Customer customer;
		private List<ReceiptItem> items = new ArrayList<>();

		private Builder() {
		}

		public Builder customer(@NotNull Customer customer) {
			this.customer = customer;
			return this;
		}

		public Builder items(@NotNull List<ReceiptItem> items) {
			this.items = items;
			return this;
		}

		public Builder items(@NotNull ReceiptItem... items) {
			this.items = Arrays.asList(items);
			return this;
		}

		@Contract(" -> new")
		public @NotNull Receipt build() {
			if (items.isEmpty())
				throw new ReceiptItemsEmptyException();
			if (items.size() > 6)
				throw new ReceiptItemLimitException();
			return new Receipt(this);
		}
	}
}
