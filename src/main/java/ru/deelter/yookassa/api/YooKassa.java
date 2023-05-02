package ru.deelter.yookassa.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.yookassa.api.data.*;
import ru.deelter.yookassa.api.data.collections.PaymentList;
import ru.deelter.yookassa.api.data.collections.RefundList;
import ru.deelter.yookassa.api.data.collections.WebhookList;
import ru.deelter.yookassa.api.data.requests.*;
import ru.deelter.yookassa.api.events.YooKassaEvent;
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

import static ru.deelter.yookassa.api.data.requests.RequestType.*;

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

	public Payment createPayment(@NotNull PaymentRequest request) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Payment.class, PAYMENT_CREATE.createRequest(), request);
	}

	public Payment createPayment(@NotNull Amount amount, @NotNull String description, @NotNull String redirectUrl) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return createPayment(PaymentRequest.create(amount, redirectUrl, description));
	}

	public Payment getPayment(@NotNull UUID uuid) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Payment.class, PAYMENT_GET.createRequest(uuid), null);
	}

	public PaymentList getPayments() throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(PaymentList.class, PAYMENT_GET_ALL.createRequest(), null);
	}

	public Refund createRefund(@NotNull UUID paymentIdentifier, @NotNull Amount amount) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Refund.class, REFUND_CREATE.createRequest(), new RefundRequest(amount, paymentIdentifier));
	}

	public Refund getRefund(@NotNull UUID uuid) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Refund.class, REFUND_GET.createRequest(uuid), null);
	}

	public RefundList getRefunds() throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(RefundList.class, REFUNDS_GET.createRequest(), null);
	}

	public Webhook createWebhook(@NotNull YooKassaEvent event, @NotNull String url) throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(Webhook.class, WEBHOOK_CREATE.createRequest(), new WebhookRequest(event, url));
	}

	public void deleteWebhook(@NotNull UUID uuid) throws UnspecifiedShopInformation, BadRequestException, IOException {
		parseResponse(null, WEBHOOK_DELETE.createRequest(uuid), null);
	}

	public WebhookList getWebhooks() throws UnspecifiedShopInformation, BadRequestException, IOException {
		return parseResponse(WebhookList.class, WEBHOOK_GET_ALL.createRequest(), null);
	}

	private <T extends IYooObject> @Nullable T parseResponse(@Nullable Class<T> yooClazz, @NotNull YooRequest request, @Nullable IYooRequestData requestData) throws IOException, UnspecifiedShopInformation, BadRequestException {
		if (shopId == 0 || token == null)
			throw new UnspecifiedShopInformation();

		HttpURLConnection connection = (HttpURLConnection) new URL(request.getUrl()).openConnection();
		connection.setRequestMethod(request.getType().getMethod().name());

		byte[] message = (shopId + ":" + token).getBytes(StandardCharsets.UTF_8);
		String basicAuth = DatatypeConverter.printBase64Binary(message);
		connection.setRequestProperty("Authorization", "Basic " + basicAuth);

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
