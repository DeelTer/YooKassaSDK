package ru.deelter.yookassa.notifications;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Address ranges YooKassa sends HTTP notifications from, per the
 * <a href="https://yookassa.ru/developers/using-api/webhooks">notification guide</a>.
 * Verify the list against the current documentation when upgrading.
 *
 * <p>Pass the TCP peer address. Behind a reverse proxy use the address the proxy observed,
 * never a client-controlled {@code X-Forwarded-For} value from an untrusted hop.
 */
public final class YooKassaNetworks {

	public static final List<String> RANGES = Collections.unmodifiableList(Arrays.asList(
		"185.71.76.0/27",
		"185.71.77.0/27",
		"77.75.153.0/25",
		"77.75.156.11/32",
		"77.75.156.35/32",
		"77.75.154.128/25",
		"2a02:5180::/32"));

	private static final byte[][] NETWORKS = new byte[RANGES.size()][];
	private static final int[] PREFIXES = new int[RANGES.size()];

	static {
		for (int i = 0; i < RANGES.size(); i++) {
			String[] parts = RANGES.get(i).split("/");
			NETWORKS[i] = literal(parts[0]);
			PREFIXES[i] = Integer.parseInt(parts[1]);
		}
	}

	private YooKassaNetworks() {
	}

	/**
	 * True when a textual IPv4/IPv6 literal belongs to YooKassa. Host names are rejected, never resolved.
	 */
	public static boolean contains(String address) {
		if (address == null) return false;
		String value = address.trim();
		if (value.startsWith("[") && value.endsWith("]")) value = value.substring(1, value.length() - 1);
		byte[] bytes = literal(value);
		return bytes != null && contains(bytes);
	}

	public static boolean contains(InetAddress address) {
		return address != null && contains(address.getAddress());
	}

	private static boolean contains(byte[] address) {
		for (int i = 0; i < NETWORKS.length; i++) {
			if (matches(NETWORKS[i], PREFIXES[i], address)) return true;
		}
		return false;
	}

	private static boolean matches(byte[] network, int prefix, byte[] address) {
		if (network.length != address.length) return false;
		int full = prefix / 8;
		for (int i = 0; i < full; i++) {
			if (network[i] != address[i]) return false;
		}
		int rest = prefix % 8;
		if (rest == 0) return true;
		int mask = (0xFF << (8 - rest)) & 0xFF;
		return (network[full] & mask) == (address[full] & mask);
	}

	/**
	 * Parses only IP literals, so a crafted host name can never trigger a DNS lookup.
	 */
	private static byte[] literal(String value) {
		if (value.isEmpty() || value.length() > 45) return null;
		if (value.indexOf(':') < 0) return ipv4(value);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (Character.digit(c, 16) < 0 && c != ':' && c != '.') return null;
		}
		try {
			// A bracketed host must be an IPv6 literal; the JDK rejects anything else without a DNS query.
			byte[] bytes = InetAddress.getByName("[" + value + "]").getAddress();
			return bytes.length == 16 && isMappedIpv4(bytes) ? Arrays.copyOfRange(bytes, 12, 16) : bytes;
		} catch (UnknownHostException e) {
			return null;
		}
	}

	/**
	 * Strict dotted-quad parser: four decimal octets 0-255, no DNS.
	 */
	private static byte[] ipv4(String value) {
		String[] parts = value.split("\\.", -1);
		if (parts.length != 4) return null;
		byte[] bytes = new byte[4];
		for (int i = 0; i < 4; i++) {
			String part = parts[i];
			if (part.isEmpty() || part.length() > 3) return null;
			int octet = 0;
			for (int j = 0; j < part.length(); j++) {
				char c = part.charAt(j);
				if (c < '0' || c > '9') return null;
				octet = octet * 10 + (c - '0');
			}
			if (octet > 255) return null;
			bytes[i] = (byte) octet;
		}
		return bytes;
	}

	private static boolean isMappedIpv4(byte[] bytes) {
		for (int i = 0; i < 10; i++) if (bytes[i] != 0) return false;
		return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
	}
}
