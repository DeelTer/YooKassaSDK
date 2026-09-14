# YooKassa SDK for Java

Unofficial synchronous Java client for the [YooKassa API v3](https://yookassa.ru/developers/api).
It covers **all 34 operations** of the official OpenAPI specification: payments, payment methods,
invoices, refunds, receipts, payouts, SBP banks, personal data, safe deals, POS links, shop settings
and OAuth webhook subscriptions. See the [operation table](docs/API_COVERAGE.md).

**Java 8+ · OkHttp 4 · Gson · 230 generated, lossless models**

> **Version 2.0.0 is a complete rewrite and is not source-compatible with 1.x.**
> Read [Migrating from 1.x](#migrating-from-1x) before upgrading.

## Contents

- [Installation](#installation)
- [Quick start](#quick-start)
- [Client configuration](#client-configuration)
- [Idempotence and retries](#idempotence-and-retries)
- [Payments](#payments)
- [Refunds, receipts and partial capture](#refunds-receipts-and-partial-capture)
- [Pagination and filters](#pagination-and-filters)
- [Models](#models)
- [Error handling](#error-handling)
- [Incoming notifications](#incoming-notifications)
- [Webhook subscriptions (OAuth)](#webhook-subscriptions-oauth)
- [Caching reference data](#caching-reference-data)
- [Payouts and personal data](#payouts-and-personal-data)
- [Threading and resources](#threading-and-resources)
- [Migrating from 1.x](#migrating-from-1x)
- [Development](#development)

## Installation

### JitPack

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.DeelTer</groupId>
    <artifactId>YooKassaSDK</artifactId>
    <version>2.0.0</version>
</dependency>
```

Gradle: `implementation("com.github.DeelTer:YooKassaSDK:2.0.0")`.

Always pin a release tag. `master-SNAPSHOT` or a commit hash follows ongoing development and
may break without notice. Releases `1.0.0`–`1.0.6` stay available under their tags.

### Local build

```shell
mvn clean install
```

```xml
<dependency>
    <groupId>ru.deelter.yookassa</groupId>
    <artifactId>YooKassaSDK</artifactId>
    <version>2.0.0</version>
</dependency>
```

Gson and OkHttp (with the Kotlin standard library) are transitive dependencies.
**Shading into a plugin** (Minecraft servers and similar): relocate `ru.deelter.yookassa`, `okhttp3`,
`okio`, `kotlin` and `com.google.gson`. Otherwise two plugins with different SDK versions on one
server load conflicting classes.

## Quick start

```java
import java.math.BigDecimal;
import ru.deelter.yookassa.YooKassa;
import ru.deelter.yookassa.model.*;

// One client per credential set, reused for the application's lifetime.
YooKassa api = YooKassa.create(System.getenv("YOOKASSA_SHOP_ID"), System.getenv("YOOKASSA_SECRET_KEY"));

CreatePaymentRequest request = new CreatePaymentRequest()
        .setAmount(MonetaryAmount.of(new BigDecimal("100.00"), MonetaryAmount.CURRENCY_RUB))
        .setDescription("Order #123")
        .setCapture(true)
        .setConfirmation(new ConfirmationDataRedirect()
                .setType(ConfirmationDataRedirect.TYPE_REDIRECT)
                .setReturnUrl("https://example.com/orders/123"));

// Generate once and store with the order BEFORE sending.
String key = YooKassa.newIdempotenceKey();
Payment payment = api.createPayment(request, key);

ConfirmationRedirect confirmation = payment.getConfirmation().as(ConfirmationRedirect.class);
String payUrl = confirmation.getConfirmationUrl(); // redirect the customer here

// At application shutdown, after requests have finished:
api.close();
```

Keep credentials on the server. Never log the secret key, OAuth token, `Authorization` headers,
raw response bodies or customer data.

## Client configuration

```java
import java.time.Duration;
import ru.deelter.yookassa.RetryPolicy;

YooKassa api = YooKassa.builder()
        .shop(shopId, secretKey)               // or .oauth(token)
        .baseUrl("https://api.yookassa.ru/v3") // default; a proxy or a mock server in tests
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(40))
        .callTimeout(Duration.ofSeconds(60))   // per attempt
        .retryPolicy(RetryPolicy.defaults())
        .build();
```

| Setting | Default |
| --- | --- |
| `baseUrl` | `YooKassa.DEFAULT_BASE_URL` (`https://api.yookassa.ru/v3`) |
| `connectTimeout` / `readTimeout` / `callTimeout` | 10 s / 40 s / 60 s |
| `retryPolicy` | `RetryPolicy.none()` |
| `httpClient` | a new SDK-owned OkHttp client |

Shortcuts: `YooKassa.create(shopId, secretKey)` and `YooKassa.oauth(token)`.
`shop(...)` accepts the shop ID as `String` or `long`. Blank values and control characters are
rejected immediately, before any request is sent.

`httpClient(okHttpClient)` shares your own OkHttp client (proxy, interceptors, certificate pinning).
The caller owns it: `close()` does not shut it down. Explicit timeouts are applied to a derived
client that shares its connection pool and dispatcher.

## Idempotence and retries

Every write operation (`POST`, `DELETE`) takes a `String idempotenceKey`: 1–64 visible ASCII characters.

- Create a key with `YooKassa.newIdempotenceKey()` (or use your own operation ID) and **persist it
  before the first attempt**.
- Retry one operation with **the same key and exactly the same payload**.
- A new operation (for example, a second refund) needs a new key.
- YooKassa keeps idempotence for 24 hours
  ([interaction format](https://yookassa.ru/developers/using-api/interaction-format)).

`RetryPolicy` repeats one call automatically:

| Retried | Not retried |
| --- | --- |
| transport failures and timeouts | HTTP 4xx except 429 |
| HTTP 429 | a 2xx response with an unreadable body |
| HTTP 5xx | an interrupted thread or a closed client |

Each attempt reuses the serialized payload and the key. The delay is exponential with jitter.
The server's `retry_after` value is used when it is longer, capped by `maxDelay`.

```java
RetryPolicy policy = RetryPolicy.builder()
        .maxAttempts(4)                        // including the first attempt
        .initialDelay(Duration.ofMillis(300))
        .maxDelay(Duration.ofSeconds(5))
        .build();
```

Retries block the calling thread. If the last attempt fails with a timeout or 5xx, the outcome is
**unknown**. Read the object's status before creating another operation. Never change the key for a retry.

## Payments

```java
Payment current = api.getPayment(paymentId);
boolean paid = Payment.STATUS_SUCCEEDED.equals(current.getStatus());

// Two-step payment (capture=false) in waiting_for_capture:
Payment captured = api.capturePayment(paymentId, new PaymentCaptureRequest(), captureKey); // full amount
Payment canceled = api.cancelPayment(paymentId, cancelKey);                                // or cancel
```

Capture and cancellation are alternatives. Fulfill an order only after checking the payment status,
amount, currency and its link to your order through the API. A browser redirect back to your site is not proof of payment.

## Refunds, receipts and partial capture

```java
ReceiptDataItem item = new ReceiptDataItem()
        .setDescription("Coffee beans")
        .setQuantity(new BigDecimal("0.250"))
        .setMeasure(ReceiptDataItem.MEASURE_KILOGRAM)
        .setVatCode(1L)
        .setPaymentMode(ReceiptDataItem.PAYMENT_MODE_FULL_PAYMENT)
        .setPaymentSubject(ReceiptDataItem.PAYMENT_SUBJECT_COMMODITY)
        .setAmount(MonetaryAmount.of(new BigDecimal("25.00"), MonetaryAmount.CURRENCY_RUB));

ReceiptData receipt = new ReceiptData()
        .setCustomer(new ReceiptDataCustomer().setEmail("customer@example.com"))
        .setItems(java.util.Collections.singletonList(item));

Refund refund = api.createRefund(new RefundRequest()
        .setPaymentId(paymentId)
        .setAmount(MonetaryAmount.of(new BigDecimal("25.00"), MonetaryAmount.CURRENCY_RUB))
        .setReceipt(receipt), refundKey);

Payment partial = api.capturePayment(paymentId, new PaymentCaptureRequest()
        .setAmount(MonetaryAmount.of(new BigDecimal("75.00"), MonetaryAmount.CURRENCY_RUB))
        .setReceipt(receipt), captureKey);
```

Standalone receipts use `PostReceiptData` with `api.createReceipt(...)`.
Receipt rules (item limits: 80 for "Receipts from YooKassa", 100 for third-party registers; VAT codes,
measures, customer contact) depend on your fiscal provider. The API validates them; check the
[receipt parameters](https://yookassa.ru/developers/payment-acceptance/receipts/54fz/other-services/parameters-values).

## Pagination and filters

```java
import java.time.OffsetDateTime;

String cursor = null;
do {
    PaymentList page = api.getPayments(new GetPaymentsQuery()
            .setLimit(50L)
            .setStatus(Payment.STATUS_SUCCEEDED)
            .setCreatedAtGteAsDateTime(OffsetDateTime.parse("2026-09-01T00:00:00Z"))
            .setCursor(cursor));
    for (Payment entry : page.getItems()) {
        // process entry
    }
    cursor = page.getNextCursor();
} while (cursor != null);
```

Query objects expose every documented filter. Pass `null` to omit all filters. Cursors are opaque
strings: pass them back unchanged. Payout metadata search uses `deepObject` encoding:

```java
JsonObject metadata = new JsonObject();
metadata.addProperty("operation_id", "my-payout-123");
PayoutsList found = api.searchPayouts(new SearchPayoutsQuery().setMetadata(metadata));
// -> GET /payouts/search?metadata[operation_id]=my-payout-123
```

## Models

All models live in `ru.deelter.yookassa.model` and are generated from the specification.

- **Fluent setters and typed getters.** Optional fields stay absent until set; passing `null` removes a field.
- **Constants for documented values**, for example `Payment.STATUS_SUCCEEDED`, `MonetaryAmount.CURRENCY_RUB`,
  `Webhook.EVENT_PAYMENT_SUCCEEDED`. Getters still return `String`, so values added later by YooKassa never break parsing.
- **Dates.** `date-time` fields have `getXxxAsDateTime()` / `setXxxAsDateTime(OffsetDateTime)`; `date`
  fields have `...AsDate` with `LocalDate`. String accessors return the exact API value (microseconds, offsets).
- **Money.** `MonetaryAmount.value` is a decimal string, never a `double`. Use
  `MonetaryAmount.of(BigDecimal, currency)`, `getValueAsDecimal()` and `setValueAsDecimal(BigDecimal)`.
- **Lossless.** Unknown fields, including nested ones, survive parsing and serialization.
  `getField(name)`, `setField(name, value)` and `toJsonObject()` access them.
- **Detached copies.** Getters, setters and constructors copy data. Change a nested object, then
  set it back. Each nested getter call builds a new object, so keep the result in a local variable inside loops.
- **Polymorphic fields** (`payment_method`, `confirmation`, payout destinations) are `JsonModel`.
  Check the discriminator, then take a typed view:

```java
JsonModel method = payment.getPaymentMethod();
JsonElement type = method == null ? null : method.getField("type");
if (type != null && "bank_card".equals(type.getAsString())) {
    String last4 = method.as(PaymentMethodBankCard.class).getCard().getLast4();
}
```

- `toString()` prints only the class and field names, never values, because objects contain personal data.
  Use `toJson()` deliberately. `equals` compares class and JSON content.
- Models are mutable and not thread-safe. Do not change a request between retries of one operation.
- Parse stored JSON with `JsonUtil.fromJson(json, Payment.class)`.

Models describe the wire format; they are not a complete validator. The API checks required
fields, allowed combinations and store-specific rules.

## Error handling

```java
import java.io.IOException;
import ru.deelter.yookassa.exceptions.*;

try {
    Payment result = api.createPayment(request, key);
} catch (InvalidRequestException e) {                     // 400: fix the request
    String parameter = e.getParameter();
} catch (AuthenticationException | ForbiddenException e) { // 401 / 403: credentials or permissions
    // alert, do not retry
} catch (YooKassaApiException e) {                         // 404, 429, 5xx and other non-2xx
    int status = e.getStatusCode();
    String code = e.getCode();
    String requestId = e.getRequestId();                   // quote it to YooKassa support
    Long retryAfter = e.getRetryAfterMillis();
    boolean retryable = e.isRetryable();                   // 429 or 5xx
} catch (IOException e) {
    // transport failure or unreadable 2xx body: the outcome may be unknown
}
```

| Exception | When |
| --- | --- |
| `InvalidRequestException` | HTTP 400 |
| `AuthenticationException` | HTTP 401 |
| `ForbiddenException` | HTTP 403 |
| `NotFoundException` | HTTP 404 |
| `TooManyRequestsException` | HTTP 429 |
| `ServerErrorException` | HTTP 5xx |
| `YooKassaApiException` | base class, and any other non-2xx status |
| `YooKassa.ResponseParseException` (`IOException`) | 2xx with a missing or malformed body |
| `IOException` | network failure, timeout |
| `IllegalArgumentException` / `NullPointerException` | invalid arguments, detected before sending |
| `IllegalStateException` | a call on a closed client, or a builder without credentials |

`YooKassaApiException` is unchecked. Its message contains only the HTTP status. `getResponseBody()`
returns raw server data, which can contain personal data: redact it before logging.

## Incoming notifications

```java
import ru.deelter.yookassa.notifications.WebhookNotification;
import ru.deelter.yookassa.notifications.YooKassaNetworks;

// In your HTTP handler:
if (!YooKassaNetworks.contains(remoteAddress)) {
    return 403;                                  // TCP peer address, not a client-supplied X-Forwarded-For
}
WebhookNotification notification = WebhookNotification.parse(requestBody);
if (notification.isPaymentEvent()) {
    Payment payment = api.getPayment(notification.getObjectId()); // trust the API, not the notification body
    if (Payment.STATUS_SUCCEEDED.equals(payment.getStatus())) {
        // fulfill the order once (handlers must be idempotent)
    }
}
return 200;
```

- `WebhookNotification` exposes `getEvent()`, `getObjectType()`, `getObjectId()`, `getPayment()`,
  `getRefund()` and `getObject(Class)`.
- `YooKassaNetworks.contains(...)` accepts IPv4/IPv6 literals, including `[..]` and IPv4-mapped IPv6.
  It never resolves host names. The ranges are listed in `YooKassaNetworks.RANGES`; compare them with the
  [notification guide](https://yookassa.ru/developers/using-api/webhooks) when upgrading.
- Behind a reverse proxy, use the client address from a proxy you trust.
- The same notification can arrive several times. Answer with HTTP 200 quickly.

## Webhook subscriptions (OAuth)

Managing subscriptions through the API requires an OAuth token from the partner flow. Shops that use
a shop ID and secret key configure notifications in the merchant dashboard instead.

```java
try (YooKassa partner = YooKassa.oauth(System.getenv("YOOKASSA_OAUTH_TOKEN"))) {
    Webhook webhook = partner.createWebhook(new CreateWebhookRequest()
            .setEvent(Webhook.EVENT_PAYMENT_SUCCEEDED)
            .setUrl("https://example.com/webhooks/yookassa"), YooKassa.newIdempotenceKey());
    WebhookList all = partner.getWebhooks();
    partner.deleteWebhook(webhook.getId(), YooKassa.newIdempotenceKey());
}
```

## Caching reference data

```java
import ru.deelter.yookassa.cache.ReferenceDataCache;

ReferenceDataCache references = ReferenceDataCache.withDefaults(api); // SBP banks: 12 h, settings: 10 min
GetSbpBanksResponse banks = references.getSbpBanks();
Me settings = references.getSettings(null);
references.invalidateAll();                                           // for example after changing settings
```

Only rarely changing reference data is cacheable. Each call returns a detached copy. Payments,
refunds, payouts and deals change status and must always be read from the API.

## Payouts and personal data

Payouts use the credentials of a payout gateway, not those of a payment shop.

```java
PayoutRequest payout = new PayoutRequest()
        .setAmount(MonetaryAmount.of(new BigDecimal("100.00"), MonetaryAmount.CURRENCY_RUB))
        .setPayoutDestinationData(new PayoutToSbpDestinationData()
                .setType("sbp").setPhone(recipientPhone).setBankId(bankId));
Payout result = api.createPayout(payout, payoutKey);

PersonalData person = api.createPersonalData(new SbpPayoutRecipientPersonalDataRequest()
        .setType("sbp_payout_recipient")
        .setFirstName(firstName).setLastName(lastName), personalDataKey);
```

Complete the requests according to your gateway's requirements and the current payout documentation.

## Threading and resources

- `YooKassa` is thread-safe. Share one instance per credential set.
- All methods block. Do not call them on UI, event-loop or game tick threads; use your own executor.
- Every HTTP response is closed on success and on failure.
- `close()` releases SDK-owned threads and connections and rejects further calls. Call it after
  in-flight requests finish. An injected OkHttp client is left open.

## Migrating from 1.x

2.0.0 removes the 1.x API. Nothing from 1.x is kept for compatibility, so plan code changes.
Projects that cannot migrate now should stay on tag `1.0.6`.

| 1.x | 2.0.0 |
| --- | --- |
| `YooKassa.create(int, String)`, `new YooKassa(...)` | `YooKassa.create(String, String)`, `YooKassa.builder()` |
| `YooKassa.withOAuth(token[, client])` | `YooKassa.oauth(token)`, `builder().oauth(token).httpClient(client)` |
| `data.impl.Payment`, `Refund`, `Webhook`, `Receipt`, `ReceiptItem`, `Customer` | `model.Payment`, `Refund`, `Webhook`, `ReceiptData`, `ReceiptDataItem`, `ReceiptDataCustomer` |
| `PaymentCreateData.builder()...build()` | `new CreatePaymentRequest().set...()` |
| `Amount.from(100, Currency.RUB)` | `MonetaryAmount.of(new BigDecimal("100.00"), MonetaryAmount.CURRENCY_RUB)` |
| `PaymentStatus.SUCCEEDED`, `payment.getStatus().isSuccess()` | `Payment.STATUS_SUCCEEDED.equals(payment.getStatus())` |
| `YooKassaEvent.PAYMENT_SUCCESS_PAID` | `Webhook.EVENT_PAYMENT_SUCCEEDED` |
| `UUID` IDs and keys | `String` IDs and keys (`YooKassa.newIdempotenceKey()`) |
| methods without a key (random key per call) | a key is always required for writes |
| `PaymentListRequest.builder()`, `RefundListRequest` | `new GetPaymentsQuery()`, `new GetRefundsQuery()` |
| `payment.getRedirectUrl()` | `payment.getConfirmation().as(ConfirmationRedirect.class).getConfirmationUrl()` |
| `BadRequestException` | `YooKassaApiException` and its subclasses |
| `InvalidEmailException`, `InvalidInnLengthException`, `ReceiptItemLimitException` and other local validators | removed; the API returns `InvalidRequestException` |
| `getToken()`, `YooRequestUrls`, `CustomerUtil`, `IYoo*`, `requests.*` | removed |

New in 2.0.0: all 34 operations, retries, configurable base URL and timeouts, status-specific
exceptions with `retry_after`, typed dates and amounts, value constants, notification parsing and
sender checks, a reference data cache, faster model access, and source and Javadoc JARs.

## Development

```shell
mvn clean verify
```

Tests intercept HTTP in memory: no credentials, real payments or network access. Every operation
in the specification is checked for method, path encoding, headers, body and response parsing.

Models and operations are generated. Do not edit `YooKassaOperations.java` or `model/*`
(except `JsonModel.java`) by hand:

```shell
python tools/generate_v3.py
git diff --exit-code -- src/main/java/ru/deelter/yookassa/model src/main/java/ru/deelter/yookassa/YooKassaOperations.java
mvn clean verify
```

- `openapi/yookassa.json` is the official specification converted from YAML without changes.
- `openapi/coverage.json` stores its SHA-256, operation signatures and model names.
- A new API operation stops generation until a Java method name is added to `OPERATIONS` in the generator.
- The generator does not delete files; remove models of deleted schemas manually.

Python (standard library only) is needed only to regenerate sources.

Integration with a YooKassa test shop is still required for your payment flows, receipts and notifications.

References: [API reference](https://yookassa.ru/developers/api) ·
[OpenAPI specification](https://yookassa.ru/developers/using-api/openapi-specification) ·
[Interaction format](https://yookassa.ru/developers/using-api/interaction-format) ·
[Notifications](https://yookassa.ru/developers/using-api/webhooks)
