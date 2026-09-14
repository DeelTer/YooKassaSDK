package ru.deelter.yookassa;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import org.junit.Test;
import ru.deelter.yookassa.cache.ReferenceDataCache;
import ru.deelter.yookassa.exceptions.*;
import ru.deelter.yookassa.model.*;
import ru.deelter.yookassa.notifications.WebhookNotification;
import ru.deelter.yookassa.notifications.YooKassaNetworks;
import ru.deelter.yookassa.utils.JsonUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/** Client behavior: auth, base URL, timeouts, retries, errors, models, caching and notifications. */
public class ClientFeaturesTest {
	private static final String PAYMENT = "{\"id\":\"pay-1\",\"status\":\"succeeded\",\"amount\":{\"value\":\"100.00\",\"currency\":\"RUB\"},"
			+ "\"created_at\":\"2026-09-14T10:00:00.123456Z\",\"metadata\":{\"order\":\"72\"}}";

	/** Replays scripted responses; a null status throws a transport failure. Tracks body closing. */
	private static final class Script {
		final List<Request> requests = new ArrayList<>();
		final List<String> payloads = new ArrayList<>();
		final Deque<Object[]> responses = new ArrayDeque<>();
		final AtomicBoolean closed = new AtomicBoolean();

		Script then(Integer status, String body) {
			responses.add(new Object[] {status, body});
			return this;
		}

		OkHttpClient client() {
			return new OkHttpClient.Builder().addInterceptor(chain -> {
				requests.add(chain.request());
				Buffer payload = new Buffer();
				if (chain.request().body() != null) chain.request().body().writeTo(payload);
				payloads.add(payload.readUtf8());
				Object[] next = responses.isEmpty() ? new Object[] {200, PAYMENT} : responses.poll();
				if (next[0] == null) throw new IOException("connection reset");
				closed.set(false);
				BufferedSource source = Okio.buffer(new ForwardingSource(new Buffer().writeUtf8((String) next[1])) {
					@Override public void close() throws IOException {
						closed.set(true);
						super.close();
					}
				});
				ResponseBody body = new ResponseBody() {
					@Override public MediaType contentType() { return YooKassa.MEDIA_TYPE_JSON; }
					@Override public long contentLength() { return -1; }
					@Override public BufferedSource source() { return source; }
				};
				return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
						.code((Integer) next[0]).message("test").body(body).build();
			}).build();
		}

		YooKassa api() {
			return YooKassa.builder().shop(123, "secret").httpClient(client()).build();
		}
	}

	private static RetryPolicy retries(int attempts, List<Long> sleeps) {
		return RetryPolicy.builder().maxAttempts(attempts).jitter(false)
				.initialDelay(Duration.ofMillis(100)).maxDelay(Duration.ofSeconds(5)).sleeper(sleeps::add).build();
	}

	private static CreatePaymentRequest payment() {
		return new CreatePaymentRequest().setAmount(MonetaryAmount.of(new BigDecimal("100.00"), MonetaryAmount.CURRENCY_RUB));
	}

	@Test public void basicAuthKeysBodiesAndClosing() throws Exception {
		Script script = new Script();
		YooKassa api = script.api();
		api.createPayment(payment(), "order-42");
		api.createPayment(payment(), "order-42");
		Request first = script.requests.get(0);
		assertEquals("Basic MTIzOnNlY3JldA==", first.header("Authorization"));
		assertEquals("order-42", first.header("Idempotence-Key"));
		assertEquals(script.payloads.get(0), script.payloads.get(1));
		assertEquals("{\"amount\":{\"value\":\"100.00\",\"currency\":\"RUB\"}}", script.payloads.get(0));
		assertTrue(script.closed.get());
		api.getPayment("pay-1");
		assertNull(script.requests.get(2).header("Idempotence-Key"));
		assertNull(script.requests.get(2).body());
		api.cancelPayment("pay-1", "cancel-1");
		assertEquals("{}", script.payloads.get(3));
	}

	@Test public void emptyDeletionResponseAndOAuth() throws Exception {
		Script script = new Script().then(200, "");
		YooKassa api = YooKassa.builder().oauth("oauth-secret").httpClient(script.client()).build();
		api.deleteWebhook("wh-1", YooKassa.newIdempotenceKey());
		assertEquals("Bearer oauth-secret", script.requests.get(0).header("Authorization"));
		assertEquals("DELETE", script.requests.get(0).method());
		assertTrue(script.closed.get());
	}

	@Test public void credentialsAndKeysAreValidatedBeforeSending() {
		assertThrows(IllegalStateException.class, () -> YooKassa.builder().build());
		assertThrows(IllegalArgumentException.class, () -> YooKassa.builder().shop(0, "secret"));
		assertThrows(IllegalArgumentException.class, () -> YooKassa.builder().shop("1:2", "secret"));
		assertThrows(IllegalArgumentException.class, () -> YooKassa.builder().shop("1", " "));
		assertThrows(IllegalArgumentException.class, () -> YooKassa.builder().oauth("token\r\nX-Evil: 1"));
		assertThrows(IllegalArgumentException.class, () -> YooKassa.builder().oauth("токен"));
		assertThrows(NullPointerException.class, () -> YooKassa.builder().shop("1", null));
		Script script = new Script();
		YooKassa api = script.api();
		assertThrows(NullPointerException.class, () -> api.createPayment(payment(), null));
		assertThrows(IllegalArgumentException.class, () -> api.createPayment(payment(), "bad key"));
		assertThrows(IllegalArgumentException.class, () -> api.getPayment(".."));
		assertTrue(script.requests.isEmpty());
	}

	@Test public void configuredBaseUrlAndDefaults() throws Exception {
		Script script = new Script();
		YooKassa api = YooKassa.builder().shop("1", "secret").httpClient(script.client())
				.baseUrl("http://localhost:8080/mock/v3/").build();
		api.getPayment("pay/1");
		assertEquals("http://localhost:8080/mock/v3/payments/pay%2F1", script.requests.get(0).url().toString());
		assertEquals(YooKassa.DEFAULT_BASE_URL, YooKassa.create("1", "s").getBaseUrl().toString());
	}

	@Test public void explicitTimeoutsDeriveFromInjectedClientWithoutOwningIt() {
		OkHttpClient injected = new OkHttpClient();
		YooKassa api = YooKassa.builder().oauth("token").httpClient(injected).callTimeout(Duration.ofSeconds(5)).build();
		assertEquals(5000, api.httpClient().callTimeoutMillis());
		assertSame(injected.connectionPool(), api.httpClient().connectionPool());
		api.close();
		api.close();
		assertFalse(injected.dispatcher().executorService().isShutdown());
		assertThrows(IllegalStateException.class, () -> api.getPayment("pay-1"));
		injected.dispatcher().executorService().shutdown();
	}

	@Test public void ownedClientHasBoundedTimeoutsAndCloses() {
		YooKassa api = YooKassa.oauth("token");
		assertEquals(60000, api.httpClient().callTimeoutMillis());
		assertEquals(40000, api.httpClient().readTimeoutMillis());
		assertEquals(10000, api.httpClient().connectTimeoutMillis());
		api.close();
		assertTrue(api.httpClient().dispatcher().executorService().isShutdown());
	}

	@Test public void retriesTransportFailuresAndServerErrorsWithTheSameKeyAndBody() throws Exception {
		Script script = new Script().then(null, "").then(503, "{\"type\":\"error\",\"code\":\"internal_server_error\"}").then(200, PAYMENT);
		List<Long> sleeps = new ArrayList<>();
		YooKassa api = YooKassa.builder().shop(1, "secret").httpClient(script.client()).retryPolicy(retries(3, sleeps)).build();
		assertEquals("pay-1", api.createPayment(payment(), "order-42").getId());
		assertEquals(3, script.requests.size());
		for (Request request : script.requests) assertEquals("order-42", request.header("Idempotence-Key"));
		assertEquals(script.payloads.get(0), script.payloads.get(2));
		assertEquals(Arrays.asList(100L, 200L), sleeps);
	}

	@Test public void retryAfterHintAndAttemptLimitAreRespected() {
		String limited = "{\"type\":\"error\",\"code\":\"too_many_requests\",\"retry_after\":1800}";
		Script script = new Script().then(429, limited).then(429, limited);
		List<Long> sleeps = new ArrayList<>();
		YooKassa api = YooKassa.builder().shop(1, "secret").httpClient(script.client()).retryPolicy(retries(2, sleeps)).build();
		TooManyRequestsException error = assertThrows(TooManyRequestsException.class, () -> api.getPayment("pay-1"));
		assertEquals(Long.valueOf(1800), error.getRetryAfterMillis());
		assertEquals(Collections.singletonList(1800L), sleeps);
		assertEquals(2, script.requests.size());
	}

	@Test public void clientErrorsAndInvalidSuccessBodiesAreNotRetried() {
		Script script = new Script().then(400, "{\"code\":\"invalid_request\"}").then(200, "{");
		List<Long> sleeps = new ArrayList<>();
		YooKassa api = YooKassa.builder().shop(1, "secret").httpClient(script.client()).retryPolicy(retries(5, sleeps)).build();
		assertThrows(InvalidRequestException.class, () -> api.getPayment("pay-1"));
		assertThrows(YooKassa.ResponseParseException.class, () -> api.getPayment("pay-1"));
		assertEquals(2, script.requests.size());
		assertTrue(sleeps.isEmpty());
	}

	@Test public void interruptedRetryWaitStopsAndKeepsInterruptFlag() {
		Script script = new Script().then(500, "").then(200, PAYMENT);
		RetryPolicy policy = RetryPolicy.builder().maxAttempts(3).sleeper(millis -> { throw new InterruptedException(); }).build();
		YooKassa api = YooKassa.builder().shop(1, "secret").httpClient(script.client()).retryPolicy(policy).build();
		try {
			assertThrows(java.io.InterruptedIOException.class, () -> api.getPayment("pay-1"));
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
		assertEquals(1, script.requests.size());
	}

	@Test public void structuredErrorsRetainDetailsAndCloseBody() {
		Script script = new Script()
				.then(400, "{\"id\":\"request-id\",\"code\":\"invalid_request\",\"description\":\"Invalid amount\",\"parameter\":\"amount\"}")
				.then(502, "<html>Proxy error</html>");
		YooKassa api = script.api();
		YooKassaApiException error = assertThrows(InvalidRequestException.class, () -> api.getPayment("pay-1"));
		assertEquals(400, error.getStatusCode());
		assertEquals("invalid_request", error.getCode());
		assertEquals("amount", error.getParameter());
		assertEquals("request-id", error.getRequestId());
		assertEquals("YooKassa HTTP 400", error.getMessage());
		assertTrue(script.closed.get());
		error = assertThrows(ServerErrorException.class, () -> api.getPayment("pay-1"));
		assertEquals(502, error.getStatusCode());
		assertNull(error.getCode());
		assertTrue(script.closed.get());
	}

	@Test public void invalidSuccessBodiesRaiseIoAndCloseResources() {
		for (String invalid : Arrays.asList("", "null", "{", "[]", "\"text\"")) {
			Script script = new Script().then(200, invalid);
			assertThrows(IOException.class, () -> script.api().getPayment("pay-1"));
			assertTrue(script.closed.get());
		}
	}

	@Test public void statusSpecificExceptions() {
		assertTrue(YooKassaApiException.of(401, "") instanceof AuthenticationException);
		assertTrue(YooKassaApiException.of(403, null) instanceof ForbiddenException);
		assertTrue(YooKassaApiException.of(404, "<html>") instanceof NotFoundException);
		assertTrue(YooKassaApiException.of(502, "").isRetryable());
		assertFalse(YooKassaApiException.of(409, "").isRetryable());
		assertEquals(YooKassaApiException.class, YooKassaApiException.of(409, "").getClass());
		assertNull(YooKassaApiException.of(400, "{\"retry_after\":\"soon\"}").getRetryAfterMillis());
	}

	@Test public void typedDatesAmountsAndConstants() {
		Payment payment = JsonUtil.fromJson(PAYMENT, Payment.class);
		assertEquals(OffsetDateTime.of(2026, 9, 14, 10, 0, 0, 123456000, ZoneOffset.UTC), payment.getCreatedAtAsDateTime());
		assertEquals(new BigDecimal("100.00"), payment.getAmount().getValueAsDecimal());
		assertEquals(Payment.STATUS_SUCCEEDED, payment.getStatus());
		assertEquals("1000", MonetaryAmount.of(new BigDecimal("1E+3"), MonetaryAmount.CURRENCY_RUB).getValue());
		GetPaymentsQuery query = new GetPaymentsQuery()
				.setCreatedAtGteAsDateTime(OffsetDateTime.of(2026, 9, 14, 12, 0, 0, 0, ZoneOffset.ofHours(3)));
		assertEquals("2026-09-14T12:00:00+03:00", query.getCreatedAtGte());
		assertNull(new Payment().getCapturedAtAsDateTime());
		assertEquals("payment.succeeded", Webhook.EVENT_PAYMENT_SUCCEEDED);
	}

	@Test public void gettersReturnDetachedCopiesIncludingJsonElements() {
		Payment payment = JsonUtil.fromJson(PAYMENT, Payment.class);
		payment.getAmount().setValue("1.00");
		assertEquals("100.00", payment.getAmount().getValue());
		SearchPayoutsQuery query = JsonUtil.fromJson("{\"metadata\":{\"order\":\"72\"}}", SearchPayoutsQuery.class);
		JsonObject metadata = query.getMetadata();
		metadata.addProperty("order", "changed");
		assertEquals("72", query.getMetadata().get("order").getAsString());
		assertThrows(JsonParseException.class, () -> JsonUtil.fromJson("{\"amount\":\"oops\"}", Payment.class).getAmount());
		assertThrows(JsonParseException.class, () -> JsonUtil.fromJson("{\"metadata\":\"oops\"}", SearchPayoutsQuery.class).getMetadata());
	}

	@Test public void referenceCacheReusesValuesUntilTtlExpires() throws Exception {
		Script script = new Script().then(200, "{\"type\":\"list\",\"items\":[{\"bank_id\":\"100000000111\",\"name\":\"Bank\"}]}")
				.then(200, "{\"type\":\"list\",\"items\":[]}");
		AtomicLong now = new AtomicLong();
		ReferenceDataCache cache = new ReferenceDataCache(script.api(), Duration.ofMinutes(1), Duration.ofMinutes(1), now::get);
		GetSbpBanksResponse first = cache.getSbpBanks();
		first.setField("items", null);
		assertEquals(1, cache.getSbpBanks().getItems().size());
		assertEquals(1, script.requests.size());
		now.addAndGet(Duration.ofMinutes(1).toNanos());
		assertEquals(0, cache.getSbpBanks().getItems().size());
		assertEquals(2, script.requests.size());
	}

	@Test public void notificationsParseAndSenderNetworksMatch() throws Exception {
		WebhookNotification notification = WebhookNotification.parse(
				"{\"type\":\"notification\",\"event\":\"payment.succeeded\",\"object\":" + PAYMENT + "}");
		assertTrue(notification.isPaymentEvent());
		assertEquals("pay-1", notification.getObjectId());
		assertEquals(Payment.STATUS_SUCCEEDED, notification.getPayment().getStatus());
		assertThrows(JsonParseException.class, () -> WebhookNotification.parse("{\"event\":\"payment.succeeded\"}"));
		assertThrows(JsonParseException.class, () -> WebhookNotification.parse("not json"));

		assertTrue(YooKassaNetworks.contains("185.71.76.31"));
		assertFalse(YooKassaNetworks.contains("185.71.76.32"));
		assertTrue(YooKassaNetworks.contains("77.75.156.11"));
		assertFalse(YooKassaNetworks.contains("77.75.156.12"));
		assertTrue(YooKassaNetworks.contains("77.75.154.200"));
		assertTrue(YooKassaNetworks.contains("[2a02:5180::1]"));
		assertTrue(YooKassaNetworks.contains("::ffff:185.71.77.1"));
		assertFalse(YooKassaNetworks.contains("2a02:5181::1"));
		for (String rejected : Arrays.asList("yookassa.ru", "185.71.76", "999.71.76.1", "185.71.76.1.", "a:b:zz", "", null))
			assertFalse(rejected, YooKassaNetworks.contains(rejected));
		assertTrue(YooKassaNetworks.contains(InetAddress.getByName("77.75.153.1")));
	}
}
