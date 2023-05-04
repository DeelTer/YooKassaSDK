package ru.deelter.yookassa.api.exceptions;

public class ReceiptItemLimitException extends RuntimeException {

	public ReceiptItemLimitException() {
		super("Receipt can contain maximum 6 items");
	}
}
