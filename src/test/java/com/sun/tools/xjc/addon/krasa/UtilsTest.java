package com.sun.tools.xjc.addon.krasa;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * @author Vojtech Krasa
 */
public class UtilsTest {
	@Test
	public void testIsNumber() throws Exception {
		Assert.assertFalse(Utils.isNumber(String.class));
		Assert.assertFalse(Utils.isNumber(IllegalStateException.class));
		Assert.assertTrue(Utils.isNumber(BigDecimal.class));
	}
}
