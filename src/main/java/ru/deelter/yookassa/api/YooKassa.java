package ru.deelter.yookassa.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.yookassa.api.data.*;
import ru.deelter.yookassa.api.data.collections.PaymentList;
import ru.deelter.yookassa.api.data.collections.RefundList;
import ru.deelter.yookassa.api.data.collections.WebhookList;
import ru.deelter.yookassa.api.data.requests.*;
import ru.deelter.yookassa.api.data.requests.payments.PaymentCreateRequest;
import ru.deelter.yookassa.api.data.requests.payments.PaymentGetRequest;
import ru.deelter.yookassa.api.data.requests.payments.PaymentListRequest;
import ru.deelter.yookassa.api.data.requests.refunds.RefundCreateRequest;
import ru.deelter.yookassa.api.data.requests.refunds.RefundGetRequest;
import ru.deelter.yookassa.api.data.requests.refunds.RefundListRequest;
import ru.deelter.yookassa.api.data.requests.webhooks.WebhookCreateRequest;
import ru.deelter.yookassa.api.data.requests.webhooks.WebhookDeleteRequest;
import ru.deelter.yookassa.api.data.requests.webhooks.WebhookListRequest;
import ru.deelter.yookassa.api.exceptions.BadRequestException;
import ru.deelter.yookassa.api.exceptions.UnspecifiedShopInformation;
import ru.deelter.yookassa.utils.JsonUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.UUID;

public class YooKassa {

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

	public Payment createPayment(@NotNull Amount amount, @NotNull String description, @NotNull String redirectUrl) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return createPayment(PaymentCreateData.create(amount, redirectUrl, description));
	}

	public Payment getPayment(@NotNull UUID uuid) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Payment.class, new PaymentGetRequest(uuid));
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

	public Refund createRefund(@NotNull UUID paymentId, @NotNull Amount amount) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return createRefund(new RefundCreateData(amount, paymentId));
	}

	public Refund getRefund(@NotNull UUID uuid) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Refund.class, new RefundGetRequest(uuid));
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

	public WebhookList getWebhooks(WebhookListRequest request) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(WebhookList.class, request);
	}

	private <T extends IYooObject> @Nullable T parseResponse(@Nullable Class<T> yooClazz, @NotNull YooRequest request) throws IOException, UnspecifiedShopInformation, BadRequestException {
		if (shopId == 0 || token == null)
			throw new UnspecifiedShopInformation();

		HttpURLConnection connection = (HttpURLConnection) new URL(request.getUrl()).openConnection();
		connection.setRequestMethod(request.getMethod().name());

		byte[] message = (shopId + ":" + token).getBytes(StandardCharsets.UTF_8);
		String basicAuth = DatatypeConverter.printBase64Binary(message);
		connection.setRequestProperty("Authorization", "Basic " + basicAuth);

		IYooRequestData requestData = request.getData();
		if (requestData != null) {
			connection.setRequestProperty("Idempotence-Key", UUID.randomUUID().toString());
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);

			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());

			writer.write(requestData.toJson());
			writer.flush();
			writer.close();
			connection.getOutputStream().close();
		}

		boolean success = connection.getResponseCode() / 100 == 2;
		InputStream responseStream = success ? connection.getInputStream() : connection.getErrorStream();

		Scanner scanner = new Scanner(responseStream).useDelimiter("\\A");
		String response = scanner.hasNext() ? scanner.next() : "";

		if (!success)
			throw new BadRequestException(response);
		if (yooClazz == null)
			return null;
		return JsonUtil.fromJson(response, yooClazz);
	}
}
