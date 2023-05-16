<img align="right" src="https://i.imgur.com/Mu7nPnY.png" height="140" width="140">

# YooKassa SDK

A library that will allow you to create smart payments using the YooKassa service

## API Initialization

The main class through which we will work with the YooKassa API

```java
private static final YooKassa YOO_KASSA = YooKassa.create(
		10000,
		"yourTokenHere"
        );
```

## Payment creation

```java
public static Payment createPayment() throws IOException {
    return YOO_KASSA.createPayment(PaymentCreateData.builder()
                .amount(Amount.from(100, Currency.RUB))
                .description("Buy a coffee")
                .redirect("https://github.com/deelter")
                .capture(true)
                .build()
    );
}
```

## Payment information

```java
public static Payment getPayment(UUID paymentId) throws IOException {
	return YOO_KASSA.getPayment(paymentId);
}

// Is payment status success
public static boolean isSuccess(UUID paymentId) throws IOException {
	return getPayment(paymentId).getStatus().isSuccess();
}
```

### Receipts

#### Customer

The buyer (client) must be registered in the object of the receipt

```java
public static Customer createCustomer(String email, String phone) {
	return Customer.builder()
		.email(email)
		.phone(phone)
		.build();
}
```

#### Items

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
public static Payment createPaymentWithReceipt(Receipt receipt)throws IOException{
	return YOO_KASSA.createPayment(PaymentCreateData.builder()
                    .amount(Amount.from(50, Currency.RUB))
                    .description("Serious chest")
                    .redirect("https://github.com/deelter")
                    .capture(true)
                    .receipt(receipt)
                    .build()
        );
}
```

## Refunds

### Refund creation

```java
public static Refund createRefund(Payment payment, Amount amount) throws IOException {
	return YOO_KASSA.createRefund(RefundCreateData.from(payment,amount));
}
```

### Getting refund object

```java
public static Refund getRefund(@NotNull UUID refundId) throws IOException {
	return YOO_KASSA.getRefund(refundId);
}
```

## Download

### Maven

```xml

<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.DeelTer</groupId>
    <artifactId>YooKassaSDK</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.DeelTer:YooKassaSDK:1.0.0'
}
```
