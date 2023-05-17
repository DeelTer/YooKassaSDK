package ru.deelter.yookassa;

import okhttp3.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.yookassa.data.*;
import ru.deelter.yookassa.data.impl.Payment;
import ru.deelter.yookassa.data.impl.Refund;
import ru.deelter.yookassa.data.impl.Webhook;
import ru.deelter.yookassa.data.impl.collections.PaymentList;
import ru.deelter.yookassa.data.impl.collections.RefundList;
import ru.deelter.yookassa.data.impl.collections.WebhookList;
import ru.deelter.yookassa.data.impl.requests.PaymentCreateData;
import ru.deelter.yookassa.data.impl.requests.RefundCreateData;
import ru.deelter.yookassa.data.impl.requests.WebhookCreateData;
import ru.deelter.yookassa.data.impl.requests.YooRequest;
import ru.deelter.yookassa.exceptions.BadRequestException;
import ru.deelter.yookassa.exceptions.UnspecifiedShopInformation;
import ru.deelter.yookassa.requests.payments.PaymentCreateRequest;
import ru.deelter.yookassa.requests.payments.PaymentGetRequest;
import ru.deelter.yookassa.requests.payments.PaymentListRequest;
import ru.deelter.yookassa.requests.refunds.RefundCreateRequest;
import ru.deelter.yookassa.requests.refunds.RefundGetRequest;
import ru.deelter.yookassa.requests.refunds.RefundListRequest;
import ru.deelter.yookassa.requests.webhooks.WebhookCreateRequest;
import ru.deelter.yookassa.requests.webhooks.WebhookDeleteRequest;
import ru.deelter.yookassa.requests.webhooks.WebhookListRequest;
import ru.deelter.yookassa.utils.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class YooKassa {

	public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
	public static final OkHttpClient CLIENT = new OkHttpClient();

	private final int shopId;
	private final String token;

	protected YooKassa(int shopId, String token) {
		this.shopId = shopId;
		this.token = token;
	}

	@Contract(value = "_, _ -> new", pure = true)
	public static @NotNull YooKassa create(int shopId, @NotNull String token) {
		return new YooKassa(shopId, token);
	}

	public int getShopId() {
		return shopId;
	}

	public String getToken() {
		return token;
	}

	public Payment createPayment(@NotNull PaymentCreateData data) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Payment.class, new PaymentCreateRequest(data));
	}

	public Payment getPayment(@NotNull UUID uuid) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Payment.class, new PaymentGetRequest(uuid));
	}

	public Payment getPayment(@NotNull IYooPayment paymentIdHolder) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return getPayment(paymentIdHolder.getId());
	}

	public PaymentList getPayments(@NotNull PaymentListRequest request) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(PaymentList.class, request);
	}

	public PaymentList getPayments() throws UnspecifiedShopInformation, BadRequestException, IOException {
		return getPayments(PaymentListRequest.builder().build());
	}

	public Refund createRefund(@NotNull RefundCreateData data) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Refund.class, new RefundCreateRequest(data));
	}

	public Refund getRefund(@NotNull UUID uuid) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Refund.class, new RefundGetRequest(uuid));
	}

	public Refund getRefund(@NotNull IYooRefund refund) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return getRefund(refund.getId());
	}

	public RefundList getRefunds(@NotNull RefundListRequest request) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(RefundList.class, request);
	}

	public Webhook createWebhook(@NotNull WebhookCreateData data) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Webhook.class, new WebhookCreateRequest(data));
	}

	public void deleteWebhook(@NotNull UUID uuid) throws UnspecifiedShopInformation, BadRequestException, IOException {
		parseResponse(null, new WebhookDeleteRequest(uuid));
	}

	public void deleteWebhook(@NotNull IYooWebhook yooWebhook) throws UnspecifiedShopInformation, BadRequestException, IOException {
		deleteWebhook(yooWebhook.getId());
	}

	public WebhookList getWebhooks(WebhookListRequest request) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(WebhookList.class, request);
	}

	private <T extends IYooObject> @Nullable T parseResponse(@Nullable Class<T> yooClazz, @NotNull YooRequest yooRequest) throws IOException, UnspecifiedShopInformation, BadRequestException {
		if (shopId == 0 || token == null)
			throw new UnspecifiedShopInformation();

		byte[] message = (shopId + ":" + token).getBytes(StandardCharsets.UTF_8);
		String basicAuth = Base64.getEncoder().encodeToString(message);

		IYooRequestData yooData = yooRequest.getData();
		RequestBody body = null;
		if (yooData != null) {
			body = RequestBody.create(yooData.toJson(), MEDIA_TYPE_JSON);
		}

		Request request = new Request.Builder()
				.url(yooRequest.getUrl())
				.header("Idempotence-Key", yooRequest.getIdempotenceId().toString())
				.header("Authorization", "Basic " + basicAuth)
				.method(yooRequest.getMethod().name(), body)
				.build();

		try (Response response = CLIENT.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful() || responseBody == null)
				throw new BadRequestException(response);

			if (yooClazz == null)
				return null;
			return JsonUtil.fromJson(responseBody.string(), yooClazz);
		}
	}
}
