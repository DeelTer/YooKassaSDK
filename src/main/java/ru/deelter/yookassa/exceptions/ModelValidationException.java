package ru.deelter.yookassa.exceptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A model does not satisfy the API specification: a required field is absent, or a value is outside
 * its documented range or length.
 *
 * <p>Messages name fields and constraints only, never field values, so they are safe to log.
 */
public class ModelValidationException extends IllegalArgumentException {

	private final List<String> violations;

	public ModelValidationException(String model, List<String> violations) {
		super(model + " violates the API specification: " + String.join("; ", violations));
		this.violations = Collections.unmodifiableList(new ArrayList<>(violations));
	}

	/**
	 * One entry per problem, for example {@code receipt.items[0].mark_quantity.denominator must be at least 1}.
	 */
	public List<String> getViolations() {
		return violations;
	}
}
