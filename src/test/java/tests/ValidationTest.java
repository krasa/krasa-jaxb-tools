package tests;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.Test;

public class ValidationTest {

	@Test
	public void testValidation() throws Exception {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();

//		Numbers object = new Numbers();
//		validator.validate(object);
	}

}
