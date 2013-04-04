package com.sun.tools.xjc.addon.krasa;

import java.lang.reflect.Field;
import java.math.BigInteger;

import com.sun.codemodel.JFieldVar;

/**
 * @author Vojtech Krasa
 */
public class Utils {
	public static final String[] NUMBERS = new String[] { "BigDecimal", "BigInteger", "String", "byte", "short", "int",
			"long" };

	public static int toInt(Object maxOccurs) {
		if (maxOccurs instanceof BigInteger) {
			// xjc
			return ((BigInteger) maxOccurs).intValue();
		} else if (maxOccurs instanceof Integer) {
			// cxf-codegen
			return (Integer) maxOccurs;
		} else {
			throw new IllegalArgumentException("unknown type " + maxOccurs.getClass());
		}
	}

	public static Field getSimpleField(String fieldName, Class<?> clazz) {
		Class<?> tmpClass = clazz;
		try {
			do {
				for (Field field : tmpClass.getDeclaredFields()) {
					String candidateName = field.getName();
					if (!candidateName.equals(fieldName)) {
						continue;
					}
					field.setAccessible(true);
					return field;
				}
				tmpClass = tmpClass.getSuperclass();
			} while (clazz != null);
		} catch (Exception e) {
			System.out.println("Field '" + fieldName + "' not found on class " + clazz);
		}
		return null;
	}

	public static Object getField(String path, Object oo) {
		try {
			if (path.contains(".")) {
				String field = path.substring(0, path.indexOf("."));
				Field declaredField = oo.getClass().getDeclaredField(field);
				declaredField.setAccessible(true);
				Object result = declaredField.get(oo);
				return getField(path.substring(path.indexOf(".") + 1), result);
			} else {
				Field simpleField = getSimpleField(path, oo.getClass());
				simpleField.setAccessible(true);
				return simpleField.get(oo);
			}
		} catch (Exception e) {
			System.out.println("Field " + path + " not found on " + oo.getClass().getName());
		}
		return null;
	}

	public static Integer parseInt(String value) {
		try {

			Integer i = Integer.parseInt(value);
			if (i < 2147483647 && i > -2147483648) {
				return i;
			}
		} catch (Exception e) {
			try {
				return (int) Math.round(Double.parseDouble(value));

			} catch (Exception ex) {
				;
			}

		}
		return null;

	}

	public static boolean equals(String value, long val) {
		return value.equals(BigInteger.valueOf(val).toString());
	}

	public static boolean isMin(String value) {
		return equals(value, -9223372036854775808L) || equals(value, -2147483648L);
	}

	public static boolean isMax(String value) {
		return equals(value, 9223372036854775807L) || equals(value, 2147483647L);
	}

	public static boolean isNumber(JFieldVar field) {
		for (String type : NUMBERS) {
			if (type.equalsIgnoreCase(field.type().name())) {
				return true;
			}
		}
		try {
			Class aClass = Class.forName(field.type().fullName());
			while (aClass.getSuperclass() != Object.class) {
				if (aClass.getSuperclass() == Number.class) {
					return true;
				}
			}
		} catch (Exception e) {
			// whatever
		}
		return false;
	}
}
