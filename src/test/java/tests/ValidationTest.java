//package tests;
//
//import javax.validation.ConstraintViolation;
//import javax.validation.Validation;
//import javax.validation.Validator;
//import javax.validation.ValidatorFactory;
//
//import a.Main;
//import org.junit.Test;
//
//import java.util.Set;
//
//public class ValidationTest {
//
//	@Test
//	public void testValidation() throws Exception {
//		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//		Validator validator = factory.getValidator();
//
//		Main object = new Main();
//		Set<ConstraintViolation<Main>> validate = validator.validate(object);
//		for (ConstraintViolation<Main> mainConstraintViolation : validate) {
//			System.err.println(mainConstraintViolation.getMessage() + " -- " + mainConstraintViolation);
//		}
//	}
//
//}
