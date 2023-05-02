<img align="right" src="https://media.discordapp.net/attachments/973891514021314560/1011723930781888562/book.png" height="140" width="140">

# YooKassa SDK
A library that will allow you to create smart payments using the YooKassa service

## Initialize API
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
				PaymentRequest.builder()
						.amount(new Amount(BigDecimal.valueOf(100), Currency.RUB))
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
