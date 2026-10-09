package ru.deelter.yookassa;

import okhttp3.*;
import org.junit.Test;
import ru.deelter.yookassa.exceptions.ModelValidationException;
import ru.deelter.yookassa.model.*;
import ru.deelter.yookassa.utils.JsonUtil;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/** Specification constraints: numeric ranges on access, whole-object validation, client options. */
public class ValidationTest {

	@Test public void outOfRangeNumbersFromTheApiNeverReachCallerArithmetic() {
		MarkQuantity broken = JsonUtil.fromJson("{\"numerator\":1,\"denominator\":0}", MarkQuantity.class);
		ModelValidationException error = assertThrows(ModelValidationException.class, broken::getDenominator);
		assertEquals(Collections.singletonList("denominator must be at least 1"), error.getViolations());
		assertEquals(Long.valueOf(1), broken.getNumerator());
		// The raw value stays reachable for diagnostics, and the object itself stays readable.
		assertEquals(0, broken.getField("denominator").getAsInt());
		assertEquals(Collections.singletonList("denominator must be at least 1"), broken.violations());

		assertNull(JsonUtil.fromJson("{}", MarkQuantity.class).getDenominator());
		assertEquals(Long.valueOf(100), JsonUtil.fromJson("{\"denominator\":100}", MarkQuantity.class).getDenominator());
	}

	@Test public void settersRejectOutOfRangeNumbersAndKeepThePreviousValue() {
		MarkQuantity quantity = new MarkQuantity().setNumerator(1L).setDenominator(100L);
		assertThrows(ModelValidationException.class, () -> quantity.setDenominator(0L));
		assertThrows(IllegalArgumentException.class, () -> quantity.setDenominator(-5L));
		assertThrows(ModelValidationException.class, () -> quantity.setNumerator(0L));
		assertEquals(Long.valueOf(100), quantity.getDenominator());
		quantity.setDenominator(null);
		assertNull(quantity.getDenominator());

		assertThrows(ModelValidationException.class, () -> new GetPaymentsQuery().setLimit(101L));
		assertThrows(ModelValidationException.class, () -> new GetPaymentsQuery().setLimit(0L));
		assertEquals(Long.valueOf(100), new GetPaymentsQuery().setLimit(100L).getLimit());
		assertThrows(ModelValidationException.class, () -> new ReceiptDataItem().setVatCode(13L));
		assertThrows(ModelValidationException.class, () -> new ReceiptData().setTaxSystemCode(7L));
	}

	@Test public void validateReportsRequiredFieldsAndNestedPathsWithoutValues() {
		assertEquals(Collections.singletonList("amount is required"), new CreatePaymentRequest().violations());

		String secret = "person@example.com";
		CreatePaymentRequest request = JsonUtil.fromJson("{\"amount\":{\"value\":\"10.00\"},\"description\":\"" + repeat('я', 129) + "\","
				+ "\"receipt\":{\"customer\":{\"email\":\"" + secret + "\"},\"items\":[{\"description\":\"A\",\"quantity\":1,"
				+ "\"amount\":{\"value\":\"10.00\",\"currency\":\"RUB\"},\"vat_code\":1,"
				+ "\"mark_quantity\":{\"numerator\":1,\"denominator\":0}}, 5]}}", CreatePaymentRequest.class);
		List<String> problems = request.violations();
		assertTrue(problems.toString(), problems.contains("amount.currency is required"));
		assertTrue(problems.toString(), problems.contains("description must contain at most 128 character(s)"));
		assertTrue(problems.toString(), problems.contains("receipt.items[0].mark_quantity.denominator must be at least 1"));
		assertTrue(problems.toString(), problems.contains("receipt.items[1] must be an object"));
		ModelValidationException error = assertThrows(ModelValidationException.class, request::validate);
		assertEquals(problems, error.getViolations());
		assertFalse(error.getMessage().contains(secret));
		assertTrue(error.getMessage().startsWith("CreatePaymentRequest violates the API specification: "));

		CreatePaymentRequest valid = new CreatePaymentRequest()
				.setAmount(MonetaryAmount.of(new BigDecimal("10.00"), MonetaryAmount.CURRENCY_RUB))
				.setDescription(repeat('я', 128));
		valid.validate();
		assertTrue(valid.violations().isEmpty());
	}

	@Test public void lengthsCountCodePointsAndWrongJsonTypesAreReported() {
		// 128 emoji are 256 UTF-16 units but 128 characters for the API.
		String emoji = new String(Character.toChars(0x1F600));
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < 128; i++) text.append(emoji);
		CreatePaymentRequest request = new CreatePaymentRequest()
				.setAmount(MonetaryAmount.of(BigDecimal.ONE, "RUB")).setDescription(text.toString());
		assertTrue(request.violations().toString(), request.violations().isEmpty());

		ReceiptData receipt = JsonUtil.fromJson("{\"items\":{},\"timezone\":\"x\",\"customer\":[]}", ReceiptData.class);
		List<String> problems = receipt.violations();
		assertTrue(problems.toString(), problems.contains("items must be a list"));
		assertTrue(problems.toString(), problems.contains("timezone must be a number"));
		assertTrue(problems.toString(), problems.contains("customer must be an object"));
	}

	private static String repeat(char c, int count) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < count; i++) builder.append(c);
		return builder.toString();
	}

	private static YooKassa.Builder client(AtomicInteger calls, String response) {
		OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
			calls.incrementAndGet();
			return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
					.body(ResponseBody.create(response, YooKassa.MEDIA_TYPE_JSON)).build();
		}).build();
		return YooKassa.builder().shop(1, "secret").httpClient(http);
	}

	@Test public void requestValidationIsOptInAndStopsInvalidCallsBeforeTheNetwork() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		String payment = "{\"id\":\"p\",\"status\":\"pending\"}";
		CreatePaymentRequest invalid = new CreatePaymentRequest().setDescription("no amount");

		client(calls, payment).build().createPayment(invalid, "key-1");
		assertEquals(1, calls.get());

		YooKassa strict = client(calls, payment).validateRequests(true).build();
		ModelValidationException error = assertThrows(ModelValidationException.class, () -> strict.createPayment(invalid, "key-2"));
		assertEquals(Collections.singletonList("amount is required"), error.getViolations());
		assertEquals(1, calls.get());
		strict.createPayment(new CreatePaymentRequest().setAmount(MonetaryAmount.of(BigDecimal.TEN, "RUB")), "key-3");
		assertEquals(2, calls.get());
	}

	@Test public void responseValidationIsOptInAndReportsFieldsOnly() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		String incomplete = "{\"id\":\"p\",\"status\":\"succeeded\",\"description\":\"private note\"}";

		assertEquals("succeeded", client(calls, incomplete).build().getPayment("p").getStatus());

		YooKassa strict = client(calls, incomplete).validateResponses(true).build();
		YooKassa.ResponseParseException error = assertThrows(YooKassa.ResponseParseException.class, () -> strict.getPayment("p"));
		assertTrue(error.getMessage(), error.getMessage().contains("amount is required"));
		assertFalse(error.getMessage().contains("private note"));
		assertEquals(2, calls.get());
	}
}
