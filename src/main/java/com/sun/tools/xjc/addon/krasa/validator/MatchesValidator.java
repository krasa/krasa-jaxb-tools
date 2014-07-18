package com.sun.tools.xjc.addon.krasa.validator;


import org.apache.commons.beanutils.PropertyUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatchesValidator implements ConstraintValidator<Choice, Object> {

	private String[] fields;

	public void initialize(Choice constraintAnnotation) {
		fields = constraintAnnotation.value();
	}

	public boolean isValid(Object value, ConstraintValidatorContext context) {
		List<String> filledFields = new ArrayList<String>();
		for (int i = 0; i < fields.length; i++) {
			Object fieldObj;
			try {
				fieldObj = PropertyUtils.getProperty(value, fields[i]);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			if (fieldObj != null) {
				filledFields.add(fields[i]);
			}

		}

		boolean valid = false;

		if (filledFields.size() > 1) {
			addConstraintViolation(context, "More than one field is filled. Filled fields: " + Arrays.toString(filledFields.toArray()));
		} else if (filledFields.size() == 0) {
			addConstraintViolation(context, "No field is filled.");
		} else {
			valid = true;
		}


		return valid;
	}

	private void addConstraintViolation(ConstraintValidatorContext context, String message) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}
}