# API v3 operation coverage

Generated from `openapi/yookassa.json`. Every operation in this snapshot is a method of `YooKassa`.

| HTTP | Path | Java method | Request model | Response model |
| --- | --- | --- | --- | --- |
| POST | `/payments` | `createPayment` | CreatePaymentRequest | Payment |
| GET | `/payments` | `getPayments` | GetPaymentsQuery | PaymentList |
| GET | `/payments/{payment_id}` | `getPayment` | None | Payment |
| POST | `/payments/{payment_id}/capture` | `capturePayment` | PaymentCaptureRequest | Payment |
| POST | `/payments/{payment_id}/cancel` | `cancelPayment` | None | Payment |
| POST | `/payment_methods` | `createPaymentMethod` | JsonModel | JsonModel |
| GET | `/payment_methods/{payment_method_id}` | `getPaymentMethod` | None | JsonModel |
| POST | `/invoices` | `createInvoice` | CreateInvoiceRequest | Invoice |
| GET | `/invoices/{invoice_id}` | `getInvoice` | None | Invoice |
| POST | `/refunds` | `createRefund` | RefundRequest | Refund |
| GET | `/refunds` | `getRefunds` | GetRefundsQuery | RefundList |
| GET | `/refunds/{refund_id}` | `getRefund` | None | Refund |
| POST | `/receipts` | `createReceipt` | PostReceiptData | Receipt |
| GET | `/receipts` | `getReceipts` | GetReceiptsQuery | GetReceiptsResponse |
| GET | `/receipts/{receipt_id}` | `getReceipt` | None | Receipt |
| POST | `/deals` | `createDeal` | SafeDealRequest | SafeDeal |
| GET | `/deals` | `getDeals` | GetDealsQuery | GetDealsResponse |
| GET | `/deals/{deal_id}` | `getDeal` | None | SafeDeal |
| POST | `/payouts` | `createPayout` | PayoutRequest | Payout |
| GET | `/payouts` | `getPayouts` | GetPayoutsQuery | PayoutsList |
| GET | `/payouts/search` | `searchPayouts` | SearchPayoutsQuery | PayoutsList |
| GET | `/payouts/{payout_id}` | `getPayout` | None | Payout |
| GET | `/sbp_banks` | `getSbpBanks` | None | GetSbpBanksResponse |
| POST | `/personal_data` | `createPersonalData` | JsonModel | PersonalData |
| GET | `/personal_data/{personal_data_id}` | `getPersonalData` | None | PersonalData |
| POST | `/webhooks` | `createWebhook` | CreateWebhookRequest | Webhook |
| GET | `/webhooks` | `getWebhooks` | None | WebhookList |
| DELETE | `/webhooks/{webhook_id}` | `deleteWebhook` | None | void |
| GET | `/me` | `getSettings` | GetSettingsQuery | Me |
| POST | `/pos_links` | `createPosLink` | CreatePosLinkRequest | PosLinkInfo |
| POST | `/pos_links/{pos_link_id}/recipient` | `changePosLinkRecipient` | RecipientPosLinkRequest | PosLinkInfo |
| POST | `/pos_links/{pos_link_id}/deactivate` | `deactivatePosLink` | None | PosLinkInfo |
| POST | `/pos_links/{pos_link_id}/activate` | `activatePosLink` | None | PosLinkInfo |
| GET | `/pos_links/{pos_link_id}` | `getPosLink` | None | PosLinkInfo |

The facade uses 230 object models. Primitive aliases use Java strings, booleans, long integers and BigDecimal. Polymorphic unions use JsonModel and typed variant views.
