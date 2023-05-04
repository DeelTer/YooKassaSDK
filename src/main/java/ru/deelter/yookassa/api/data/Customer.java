package ru.deelter.yookassa.api.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.deelter.yookassa.api.exceptions.EmptyRequiredFieldsException;
import ru.deelter.yookassa.api.exceptions.InvalidEmailException;
import ru.deelter.yookassa.api.exceptions.InvalidInnLengthException;
import ru.deelter.yookassa.api.exceptions.InvalidPhoneException;
import ru.deelter.yookassa.utils.CustomerUtil;

/*
	"customer":{
		"fullName": "Джамалдиев Роман Зелимханович",
		"inn": "123456789123",
		"email": "deelter@gmail.com",
		"phone": "70000000000"
	}
 */
public class Customer implements IYooObject {

	@SerializedName("full_name")
	private final String fullName;
	private final String inn;
	private final String email;
	private final String phone;

	@Contract(pure = true)
	private Customer(@NotNull Builder builder) {
		fullName = builder.fullName;
		inn = builder.inn;
		email = builder.email;
		phone = builder.phone;
	}

	@Contract(value = " -> new", pure = true)
	public static @NotNull Builder builder() {
		return new Builder();
	}

	public static @NotNull Builder builder(@NotNull Customer copy) {
		Builder builder = new Builder();
		builder.fullName = copy.getFullName();
		builder.inn = copy.getInn();
		builder.email = copy.getEmail();
		builder.phone = copy.getPhone();
		return builder;
	}

	@Nullable
	public String getFullName() {
		return fullName;
	}

	@Nullable
	public String getInn() {
		return inn;
	}

	@Nullable
	public String getEmail() {
		return email;
	}

	@Nullable
	public String getPhone() {
		return phone;
	}

	public static final class Builder {
		private String fullName;
		private String inn;
		private String email;
		private String phone;

		private Builder() {
		}

		public Builder fullName(@Nullable String fullName) {
			this.fullName = fullName;
			return this;
		}

		public Builder inn(@Nullable String inn) {
			this.inn = inn;
			return this;
		}

		public Builder email(@Nullable String email) {
			this.email = email;
			return this;
		}

		public Builder phone(@Nullable String phone) {
			this.phone = phone != null ? CustomerUtil.formatPhone(phone) : null;
			return this;
		}

		@Contract(" -> new")
		public @NotNull Customer build() {
			if (phone == null && email == null)
				throw new EmptyRequiredFieldsException();
			if (phone != null && !CustomerUtil.isValidPhone(phone))
				throw new InvalidPhoneException(phone);
			if (email != null && !CustomerUtil.isValidEmail(email))
				throw new InvalidEmailException(email);
			if (inn != null && inn.length() != 12)
				throw new InvalidInnLengthException(inn);
			return new Customer(this);
		}
	}
}
