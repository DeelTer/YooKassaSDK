package ru.deelter.yookassa.exceptions;

public class ReceiptItemsEmptyException extends RuntimeException {

	public ReceiptItemsEmptyException() {
		super("Receipt must be at least one item here");
	}
}
