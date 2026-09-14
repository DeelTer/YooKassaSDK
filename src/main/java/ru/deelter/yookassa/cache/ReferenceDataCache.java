package ru.deelter.yookassa.cache;

import ru.deelter.yookassa.YooKassa;
import ru.deelter.yookassa.model.GetSbpBanksResponse;
import ru.deelter.yookassa.model.GetSettingsQuery;
import ru.deelter.yookassa.model.JsonModel;
import ru.deelter.yookassa.model.Me;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Time-limited cache for rarely changing reference data: the SBP bank list and shop settings ({@code /me}).
 * Payments, refunds and other objects with changing status are intentionally not cacheable.
 *
 * <p>Thread-safe. Each call returns a detached copy, so callers may modify results freely.
 * Concurrent misses may fetch the same data more than once; no lock is held during network I/O.
 */
public final class ReferenceDataCache {

	private static final String SBP_BANKS = "sbp_banks";

	private final YooKassa api;
	private final long sbpBanksTtlNanos;
	private final long settingsTtlNanos;
	private final LongSupplier clock;
	private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

	public ReferenceDataCache(YooKassa api, Duration sbpBanksTtl, Duration settingsTtl) {
		this(api, sbpBanksTtl, settingsTtl, System::nanoTime);
	}

	/**
	 * Uses the given monotonic nanosecond clock, for tests.
	 */
	public ReferenceDataCache(YooKassa api, Duration sbpBanksTtl, Duration settingsTtl, LongSupplier nanoClock) {
		this.api = Objects.requireNonNull(api, "api");
		this.sbpBanksTtlNanos = ttl(sbpBanksTtl, "sbpBanksTtl");
		this.settingsTtlNanos = ttl(settingsTtl, "settingsTtl");
		this.clock = Objects.requireNonNull(nanoClock, "nanoClock");
	}

	/**
	 * Twelve hours for SBP banks, ten minutes for shop settings.
	 */
	public static ReferenceDataCache withDefaults(YooKassa api) {
		return new ReferenceDataCache(api, Duration.ofHours(12), Duration.ofMinutes(10));
	}

	public GetSbpBanksResponse getSbpBanks() throws IOException {
		return get(SBP_BANKS, sbpBanksTtlNanos, GetSbpBanksResponse.class, () -> api.getSbpBanks());
	}

	/**
	 * Settings of the authenticated shop, or of {@code on_behalf_of} for OAuth partners.
	 */
	public Me getSettings(GetSettingsQuery query) throws IOException {
		String key = "me:" + (query == null ? "" : query.toJson());
		return get(key, settingsTtlNanos, Me.class, () -> api.getSettings(query));
	}

	/**
	 * Drops all cached values, for example after changing shop settings.
	 */
	public void invalidateAll() {
		entries.clear();
	}

	private <T extends JsonModel> T get(String key, long ttlNanos, Class<T> type, Loader<T> loader) throws IOException {
		long now = clock.getAsLong();
		Entry entry = entries.get(key);
		if (entry == null || now - entry.loadedAt >= ttlNanos) {
			entry = new Entry(loader.load(), clock.getAsLong());
			entries.put(key, entry);
		}
		return entry.value.as(type);
	}

	private static long ttl(Duration ttl, String name) {
		Objects.requireNonNull(ttl, name);
		if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException(name + " must be positive");
		return ttl.toNanos();
	}

	private interface Loader<T> {
		T load() throws IOException;
	}

	private static final class Entry {
		final JsonModel value;
		final long loadedAt;

		Entry(JsonModel value, long loadedAt) {
			this.value = Objects.requireNonNull(value, "value");
			this.loadedAt = loadedAt;
		}
	}
}
