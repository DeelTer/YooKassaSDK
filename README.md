<img align="right" src="https://sun6-23.userapi.com/s/v1/ig2/uGmISl3elBXZi_RPAtK7NX82UeCZGt1lsx2JixfjIqHfUMjLxFHFTkkGshLzVRFppxesDUT89gi7ucMjS1JShrgm.jpg?size=864x864&quality=95&crop=108,108,864,864&ava=1" height="140" width="140">

# YooKassa SDK
A library that will allow you to create smart payments using the YooKassa service

## Initialize instance
The main class through which we will work with the YooKassa API
```java
private static final YooKassa YOO_KASSA = YooKassa.create(
		10000,
		"yourTokenHere"
);
```
## Payment: creation
```java
public static Payment createPayment() throws IOException {
	return YOO_KASSA.createPayment(
		PaymentCreateData.builder()
				.amount(Amount.from(100, Currency.RUB))
				.description("Buy a coffee")
				.redirect("https://github.com/deelter")
				.capture(true)
				.build()
	);
}
```
## Payment: get information
```java
public static Payment getPayment(@NotNull UUID paymentId) throws IOException {
	return YOO_KASSA.getPayment(paymentId);
}
```
### Check status
```java
public static boolean isSuccess(@NotNull UUID paymentId) throws IOException {
	return YOO_KASSA.getPayment(paymentId).getStatus().isSuccess();
}
```

### Payment: receipts
#### Receipt customer
The buyer (client) must be registered in the object of the receipt
```java
public static Customer createCustomer(String email, String phone) {
	return Customer.builder()
		.email(email)
		.phone(phone)
		.build();
}
```
#### Receipt items 
The receipt object can contain either one or several items
```java
public static ReceiptItem createReceiptItem() {
	return ReceiptItem.builder()
		.description("Serious hat")
		.amount(Amount.from(50, Currency.RUB))
		.quantity(1)
		.vatCode(1)
		.build();
}
```
### Receipt integration
```java
public static Payment createPaymentWithReceipt(Receipt receipt) throws IOException {
	return YOO_KASSA.createPayment(
		PaymentCreateData.builder()
			.amount(Amount.from(100, Currency.RUB))
			.description("Buy a coffee")
			.redirect("https://github.com/deelter")
			.capture(true)
			.receipt(receipt)
			.build()
	);
}
```
