package com.sun.tools.xjc.addon.krasa.validator;

import org.junit.Assert;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

public class ChoiceValidatorTest {
	ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	Validator validator = factory.getValidator();


	@Test
	public void testSunny() throws Exception {
		DrinkType object = new DrinkType();
		object.tea = "green";
		validate(validator, object, 0);
	}

	@Test
	public void testRainy() throws Exception {
		DrinkType object = new DrinkType();
		object.tea = "green";
		object.coffee = "black";
		validate(validator, object, 1);
	}

	@Test
	public void testRainy2() throws Exception {
		DrinkType object = new DrinkType();
		validate(validator, object, 1);
	}

	private void validate(Validator validator, DrinkType object, final int expected) {
		Set<ConstraintViolation<DrinkType>> validate = validator.validate(object);
		for (ConstraintViolation<DrinkType> mainConstraintViolation : validate) {
			System.err.println(mainConstraintViolation.getMessage() + " -- " + mainConstraintViolation);
		}
		Assert.assertEquals(expected, validate.size());
	}

}