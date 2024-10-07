package ru.deelter.yookassa.utils;

public class YooRequestUrls {

	public static final String URL_API_V3 = "https://api.yookassa.ru/v3";
	public static final String PAYMENT_CREATE = URL_API_V3 + "/payments";
	public static final String PAYMENT_GET = URL_API_V3 + "/payments/%s";
	public static final String PAYMENT_GET_LIST = URL_API_V3 + "/payments";
	public static final String PAYMENT_CAPTURE = URL_API_V3 + "/payments/%s/capture";
	public static final String PAYMENT_CANCEL = URL_API_V3 + "/payments/%s/cancel";
	public static final String REFUND_CREATE = URL_API_V3 + "/refunds";
	public static final String REFUND_GET = URL_API_V3 + "/refunds/%s";
	public static final String REFUND_GET_LIST = URL_API_V3 + "/refunds";
	public static final String WEBHOOK_CREATE = URL_API_V3 + "/webhooks";
	public static final String WEBHOOK_DELETE = URL_API_V3 + "/webhooks/%s";
	public static final String WEBHOOK_GET_LIST = URL_API_V3 + "/webhooks";
}
