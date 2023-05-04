package ru.deelter.yookassa.api.exceptions;

public class ReceiptItemsEmptyException extends RuntimeException {

	public ReceiptItemsEmptyException() {
		super("Receipt must be at least one item here");
	}
}
