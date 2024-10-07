package ru.deelter.yookassa;

import okhttp3.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.yookassa.data.*;
import ru.deelter.yookassa.data.impl.Amount;
import ru.deelter.yookassa.data.impl.Payment;
import ru.deelter.yookassa.data.impl.Refund;
import ru.deelter.yookassa.data.impl.Webhook;
import ru.deelter.yookassa.data.impl.collections.PaymentList;
import ru.deelter.yookassa.data.impl.collections.RefundList;
import ru.deelter.yookassa.data.impl.collections.WebhookList;
import ru.deelter.yookassa.data.impl.requests.*;
import ru.deelter.yookassa.exceptions.BadRequestException;
import ru.deelter.yookassa.exceptions.UnspecifiedShopInformation;
import ru.deelter.yookassa.requests.payments.*;
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

	protected final int shopId;
	protected final String token;
	protected final OkHttpClient client;
	protected final String basicAuth;

	protected YooKassa(int shopId, String token) {
		this(shopId, token, new OkHttpClient());
	}

	protected YooKassa(int shopId, String token, OkHttpClient client) {
		if (shopId <= 0 || token == null)
			throw new UnspecifiedShopInformation();

		this.shopId = shopId;
		this.token = token;
		this.client = client;

		byte[] message = (shopId + ":" + token).getBytes(StandardCharsets.UTF_8);
		basicAuth = Base64.getEncoder().encodeToString(message);
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

	public Payment createPayment(@NotNull PaymentCreateData data) throws BadRequestException, IOException {
		return executeRequest(Payment.class, new PaymentCreateRequest(data));
	}

	public Payment getPayment(@NotNull UUID uuid) throws BadRequestException, IOException {
		return executeRequest(Payment.class, new PaymentGetRequest(uuid));
	}

	public Payment getPayment(@NotNull IYooPayment paymentIdHolder) throws BadRequestException, IOException {
		return getPayment(paymentIdHolder.getId());
	}

	public PaymentList getPayments(@NotNull PaymentListRequest request) throws BadRequestException, IOException {
		return executeRequest(PaymentList.class, request);
	}

	public PaymentList getPayments() throws BadRequestException, IOException {
		return getPayments(PaymentListRequest.builder().build());
	}

	public Payment capturePayment(@NotNull UUID uuid) throws BadRequestException, IOException {
		return capturePayment(uuid, null);
	}

	public Payment capturePayment(@NotNull UUID uuid, @Nullable Amount amount) throws BadRequestException, IOException {
		return executeRequest(Payment.class, new PaymentCaptureRequest(uuid, new PaymentCaptureData(amount)));
	}

	public Payment cancelPayment(@NotNull UUID uuid) throws BadRequestException, IOException {
		return executeRequest(Payment.class, new PaymentCancelRequest(uuid));
	}

	public Refund createRefund(@NotNull RefundCreateData data) throws BadRequestException, IOException {
		return executeRequest(Refund.class, new RefundCreateRequest(data));
	}

	public Refund getRefund(@NotNull UUID uuid) throws BadRequestException, IOException {
		return executeRequest(Refund.class, new RefundGetRequest(uuid));
	}

	public Refund getRefund(@NotNull IYooRefund refund) throws BadRequestException, IOException {
		return getRefund(refund.getId());
	}

	public RefundList getRefunds(@NotNull RefundListRequest request) throws BadRequestException, IOException {
		return executeRequest(RefundList.class, request);
	}

	public Webhook createWebhook(@NotNull WebhookCreateData data) throws BadRequestException, IOException {
		return executeRequest(Webhook.class, new WebhookCreateRequest(data));
	}

	public void deleteWebhook(@NotNull UUID uuid) throws BadRequestException, IOException {
		executeRequest(null, new WebhookDeleteRequest(uuid));
	}

	public void deleteWebhook(@NotNull IYooWebhook yooWebhook) throws BadRequestException, IOException {
		deleteWebhook(yooWebhook.getId());
	}

	public WebhookList getWebhooks(WebhookListRequest request) throws BadRequestException, IOException {
		return executeRequest(WebhookList.class, request);
	}

	protected <T extends IYooObject> @Nullable T executeRequest(@Nullable Class<T> yooClazz, @NotNull YooRequest yooRequest)
			throws IOException, BadRequestException {
		IYooRequestData yooData = yooRequest.getData();
		RequestBody body = yooData != null ? RequestBody.create(yooData.toJson(), MEDIA_TYPE_JSON) : null;

		Request request = new Request.Builder()
				.url(yooRequest.getUrl())
				.header("Idempotence-Key", yooRequest.getIdempotenceId().toString())
				.header("Authorization", "Basic " + basicAuth)
				.method(yooRequest.getMethod().name(), body)
				.build();

		try (Response response = client.newCall(request).execute(); ResponseBody responseBody = response.body()) {
			if (!response.isSuccessful() || responseBody == null)
				throw new BadRequestException(response);
			return yooClazz != null ? JsonUtil.fromJson(responseBody.string(), yooClazz) : null;
		}
	}
}
