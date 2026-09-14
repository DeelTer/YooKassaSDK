package ru.deelter.yookassa;

import com.google.gson.*;
import okhttp3.*;
import org.junit.Test;
import ru.deelter.yookassa.utils.JsonUtil;
import ru.deelter.yookassa.model.JsonModel;
import ru.deelter.yookassa.model.*;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ModelsTest {
    @Test public void nestedPaymentAndVariantFieldsAreAccessibleWithoutDataLoss() {
        String json = "{\"id\":\"payment-id\",\"status\":\"future-status\",\"amount\":{\"value\":\"100.00\",\"currency\":\"JPY\"},"
                + "\"payment_method\":{\"type\":\"bank_card\",\"card\":{\"last4\":\"4444\",\"future\":true}},"
                + "\"confirmation\":{\"type\":\"redirect\",\"confirmation_url\":\"https://example.com\"},\"new_field\":[1,2]}";
        Payment payment = JsonUtil.fromJson(json, Payment.class);
        assertEquals("future-status", payment.getStatus());
        assertEquals("JPY", payment.getAmount().getCurrency());
        assertEquals("4444", payment.getPaymentMethod().as(PaymentMethodBankCard.class).getCard().getLast4());
        assertEquals("https://example.com", payment.getConfirmation().as(ConfirmationRedirect.class).getConfirmationUrl());
        assertEquals(JsonParser.parseString(json), payment.toJsonObject());
    }

    @Test public void requestModelsKeepOptionalFieldsAbsentAndAmountsExact() {
        MonetaryAmount amount = new MonetaryAmount().setCurrency("RUB").setValue("10.00");
        CreatePaymentRequest request = new CreatePaymentRequest().setAmount(amount).setCapture(false)
                .setPaymentMethodId("saved-payment-method");
        amount.setValue("99.00");
        assertEquals("10.00", request.getAmount().getValue());
        assertFalse(request.getCapture());
        assertNull(request.getDescription());
        assertFalse(request.toJsonObject().has("description"));
        assertFalse(request.toJsonObject().has("json"));
        assertEquals("saved-payment-method", request.getPaymentMethodId());
    }

    @Test public void prefixedIdsAndMicrosecondTimestampsSurvive() {
        PersonalData data = JsonUtil.fromJson("{\"id\":\"pd-123\",\"created_at\":\"2026-09-14T10:00:00.123456Z\",\"type\":\"sbp_payout_recipient\"}", PersonalData.class);
        assertEquals("pd-123", data.getId());
        assertEquals("2026-09-14T10:00:00.123456Z", data.getCreatedAt());
        SafeDeal deal = JsonUtil.fromJson("{\"id\":\"dl-123\",\"balance\":{\"value\":\"-45.00\",\"currency\":\"RUB\"}}", SafeDeal.class);
        assertEquals("-45.00", deal.getBalance().getValue());
    }

    @Test public void listItemsAreTypedAndCursorsOpaque() {
        PaymentList list = JsonUtil.fromJson("{\"items\":[{\"id\":\"payment\",\"status\":\"succeeded\"}],\"next_cursor\":\"opaque+/=\"}", PaymentList.class);
        assertEquals("succeeded", list.getItems().get(0).getStatus());
        assertEquals("opaque+/=", list.getNextCursor());
        list.getItems().clear();
        assertEquals(1, list.getItems().size());
    }

    @Test public void dateAndMetadataQueryEncoding() {
        SearchPayoutsQuery query = new SearchPayoutsQuery().setCreatedAtGte("2026-09-14T12:00:00+03:00")
                .setCursor("token+/=&").setLimit(50L)
                .setMetadata(JsonParser.parseString("{\"order id\":\"a+b & c\"}").getAsJsonObject());
        HttpUrl url = RequestEncoding.url(HttpUrl.get(YooKassa.DEFAULT_BASE_URL), new String[] {"payouts", "search"}, query);
        assertEquals("/v3/payouts/search", url.encodedPath());
        assertEquals("2026-09-14T12:00:00+03:00", url.queryParameter("created_at.gte"));
        assertEquals("a+b & c", url.queryParameter("metadata[order id]"));
        assertEquals("token+/=&", url.queryParameter("cursor"));
        assertEquals("50", url.queryParameter("limit"));
    }

    @Test public void fiscalFieldsAndRefundReceiptAreSerialized() {
        ReceiptDataItem item = new ReceiptDataItem().setDescription("Product")
                .setQuantity(new BigDecimal("0.250")).setMeasure("kilogram").setVatCode(12L)
                .setAmount(new MonetaryAmount().setValue("100.00").setCurrency("RUB"));
        ReceiptData receipt = new ReceiptData().setItems(Arrays.asList(item)).setTaxSystemCode(1L);
        RefundRequest refund = new RefundRequest().setPaymentId("payment")
                .setAmount(new MonetaryAmount().setValue("25.00").setCurrency("RUB")).setReceipt(receipt);
        assertEquals(new BigDecimal("0.250"), refund.getReceipt().getItems().get(0).getQuantity());
        assertEquals(Long.valueOf(1), refund.getReceipt().getTaxSystemCode());
        assertTrue(refund.toJsonObject().getAsJsonObject("receipt").has("items"));
    }

    @Test public void keyAndPathValidationRejectHeaderOrPathInjection() {
        assertThrows(IllegalArgumentException.class, () -> RequestEncoding.key("abc\r\nAuthorization: other"));
        assertThrows(IllegalArgumentException.class, () -> RequestEncoding.key(""));
        assertThrows(IllegalArgumentException.class, () -> RequestEncoding.id(".."));
        assertThrows(IllegalArgumentException.class, () -> RequestEncoding.id(" "));
        assertEquals("/v3/part%2Fpart", RequestEncoding.url(HttpUrl.get(YooKassa.DEFAULT_BASE_URL), new String[] {RequestEncoding.id("part/part")}, null).encodedPath());
    }

    @Test public void extensionValuesRemainExactAndDetached() {
        JsonModel model = new JsonModel();
        JsonObject value = JsonParser.parseString("{\"n\":0.12345678901234567890123456789}").getAsJsonObject();
        model.setField("extension", value);
        value.addProperty("n", 0);
        assertEquals("0.12345678901234567890123456789", model.getField("extension").getAsJsonObject().get("n").getAsString());
        model.setField("extension", null);
        assertNull(model.getField("extension"));
    }
}
