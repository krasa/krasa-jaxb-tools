package com.sun.tools.xjc.addon.krasa;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.xml.sax.ErrorHandler;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CAttributePropertyInfo;
import com.sun.tools.xjc.model.CElementPropertyInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.impl.AttributeUseImpl;
import com.sun.xml.xsom.impl.ElementDecl;
import com.sun.xml.xsom.impl.ParticleImpl;
import com.sun.xml.xsom.impl.parser.DelayedRef;

/**
 * big thanks to original author: cocorossello
 */
public class JaxbValidationsPlugins extends Plugin {

	public static final String PLUGIN_OPTION_NAME = "XJsr303Annotations";
	public static final String TARGET_NAMESPACE_PARAMETER_NAME = PLUGIN_OPTION_NAME + ":targetNamespace";
	public static final String JSR_349 = PLUGIN_OPTION_NAME + ":JSR_349";
	public static final String GENERATE_NOT_NULL_ANNOTATIONS = PLUGIN_OPTION_NAME + ":generateNotNullAnnotations";

	protected String namespace = "http://jaxb.dev.java.net/plugin/code-injector";
	public String targetNamespace = "TARGET_NAMESPACE";
	public boolean jsr349 = false;
	public boolean notNullAnnotations = true;

	public static final String[] DECIMAL = new String[] { "BigDecimal", "BigInteger", "String", "byte", "short", "int",
			"long" };

	public String getOptionName() {
		return PLUGIN_OPTION_NAME;
	}

	@Override
	public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException, IOException {
		String arg1 = args[i];
		int consumed = 0;
		int indexOfNamespace = arg1.indexOf(TARGET_NAMESPACE_PARAMETER_NAME);
		if (indexOfNamespace > 0) {
			targetNamespace = arg1.substring(indexOfNamespace + TARGET_NAMESPACE_PARAMETER_NAME.length() + "=".length());
			consumed++;
		}

		int index = arg1.indexOf(JSR_349);
		if (index > 0) {
			jsr349 = Boolean.parseBoolean(arg1.substring(index + JSR_349.length() + "=".length()));
			consumed++;
		}

		int index_generateNotNullAnnotations = arg1.indexOf(GENERATE_NOT_NULL_ANNOTATIONS);
		if (index_generateNotNullAnnotations > 0) {
			notNullAnnotations = Boolean.parseBoolean(arg1.substring(index_generateNotNullAnnotations
					+ GENERATE_NOT_NULL_ANNOTATIONS.length() + "=".length()));
			consumed++;
		}

		return consumed;
	}

	public List<String> getCustomizationURIs() {
		return Collections.singletonList(namespace);
	}

	public boolean isCustomizationTagName(String nsUri, String localName) {
		return nsUri.equals(namespace) && localName.equals("code");
	}

	@Override
	public void onActivated(Options opts) throws BadCommandLineException {
		super.onActivated(opts);
	}

	public String getUsage() {
		return "  -XJsr303Annotations      :  inject Bean validation annotations (JSR 303); -XJsr303Annotations:targetNamespace=http://www.foo.com/bar  :      additional settings for @Valid annotation";
	}

	public boolean run(Outline model, Options opt, ErrorHandler errorHandler) {
		try {
			for (ClassOutline co : model.getClasses()) {

				for (CPropertyInfo property : co.target.getProperties()) {
					if (property instanceof CElementPropertyInfo) {
						processElement((CElementPropertyInfo) property, co, model);
					} else if (property instanceof CAttributePropertyInfo) {
						processAttribute((CAttributePropertyInfo) property, co, model);
					}
				}
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * XS:Element
	 */
	public void processElement(CElementPropertyInfo property, ClassOutline clase, Outline model) {
		FieldOutline field = model.getField(property);
		XSComponent schemaComponent = property.getSchemaComponent();
		ParticleImpl particle = (ParticleImpl) schemaComponent;
		int maxOccurs = toInt(getField("maxOccurs", particle));
		int minOccurs = toInt(getField("minOccurs", particle));
		JFieldVar var = clase.implClass.fields().get(getField("privateName", property));
		if (minOccurs < 0 || minOccurs >= 1) {
			if (!hasAnnotation(var, NotNull.class)) {
				if (notNullAnnotations) {
					System.out.println("@NotNull: " + property.getName() + " added to class " + clase.implClass.name());
					var.annotate(NotNull.class);
				}
			}
		}
		if (maxOccurs > 1) {
			if (!hasAnnotation(var, Size.class)) {
				System.out.println("@Size (" + minOccurs + "," + maxOccurs + ") " + property.getName()
						+ " added to class " + clase.implClass.name());
				var.annotate(Size.class).param("min", minOccurs).param("max", maxOccurs);
			}
		}
		if (maxOccurs == -1 && minOccurs > 0) { // maxOccurs="unbounded"
			if (!hasAnnotation(var, Size.class)) {
				System.out.println("@Size (" + minOccurs + ") " + property.getName() + " added to class "
						+ clase.implClass.name());
				var.annotate(Size.class).param("min", minOccurs);
			}
		}

		Object term = getField("term", particle);
		if (term instanceof ElementDecl) {
			processValidAnnotation(property, clase, var, (ElementDecl) term);
		} else if (term instanceof DelayedRef.Element) {
			XSElementDecl xsElementDecl = ((DelayedRef.Element) term).get();
			processValidAnnotation(property, clase, var, (ElementDecl) xsElementDecl);
		}

	}

	private void processValidAnnotation(CElementPropertyInfo property, ClassOutline clase, JFieldVar var,
			ElementDecl element) {
		if (element.getType().getTargetNamespace().startsWith(targetNamespace) && element.getType().isComplexType()) {
			if (!hasAnnotation(var, Valid.class)) {
				System.out.println("@Valid: " + property.getName() + " added to class " + clase.implClass.name());
				var.annotate(Valid.class);
			}
		}
		if (element.getType() instanceof XSSimpleType) {
			processType((XSSimpleType) element.getType(), var, property.getName(), clase.implClass.name());
		} else if (element.getType().getBaseType() instanceof XSSimpleType) {
			processType((XSSimpleType) element.getType().getBaseType(), var, property.getName(), clase.implClass.name());
		}
	}

	protected int toInt(Object maxOccurs) {
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

	/**
	 * XS:Attribute
	 */
	public void processAttribute(CAttributePropertyInfo property, ClassOutline clase, Outline model) {
		FieldOutline field = model.getField(property);
		System.out.println("Attribute " + property.getName() + " added to class " + clase.implClass.name());
		XSComponent definition = property.getSchemaComponent();
		AttributeUseImpl particle = (AttributeUseImpl) definition;
		JFieldVar var = clase.implClass.fields().get(getField("privateName", property));
		if (particle.isRequired()) {
			if (!hasAnnotation(var, NotNull.class)) {
				if (notNullAnnotations) {
					System.out.println("@NotNull: " + property.getName() + " added to class " + clase.implClass.name());
					var.annotate(NotNull.class);
				}
			}
		}
		if (particle.getDecl().getType().getTargetNamespace().startsWith(targetNamespace)
				&& particle.getDecl().getType().isComplexType()) {
			if (!hasAnnotation(var, Valid.class)) {
				System.out.println("@Valid: " + property.getName() + " added to class " + clase.implClass.name());
				var.annotate(Valid.class);
			}
		}
		processType(particle.getDecl().getType(), var, property.getName(), clase.implClass.name());
	}

	public void processType(XSSimpleType simpleType, JFieldVar field, String campo, String clase) {
		if (!hasAnnotation(field, Size.class)) {
			Integer maxLength = simpleType.getFacet("maxLength") == null ? null : parseInt(simpleType.getFacet(
					"maxLength").getValue().value);
			Integer minLength = simpleType.getFacet("minLength") == null ? null : parseInt(simpleType.getFacet(
					"minLength").getValue().value);
			if (maxLength != null && minLength != null) {
				System.out.println("@Size(" + minLength + "," + maxLength + "): " + campo + " added to class " + clase);
				field.annotate(Size.class).param("min", minLength).param("max", maxLength);
			} else if (minLength != null) {
				System.out.println("@Size(" + minLength + ", null): " + campo + " added to class " + clase);
				field.annotate(Size.class).param("min", minLength);
			} else if (maxLength != null) {
				System.out.println("@Size(null, " + maxLength + "): " + campo + " added to class " + clase);
				field.annotate(Size.class).param("max", maxLength);
			}
		}

		XSFacet maxInclusive = simpleType.getFacet("maxInclusive");
		if (maxInclusive != null && isNumber(field) && isValidValue(maxInclusive)
				&& !hasAnnotation(field, DecimalMax.class)) {
			System.out.println("@DecimalMax(" + maxInclusive.getValue().value + "): " + campo + " added to class "
					+ clase);
			field.annotate(DecimalMax.class).param("value", maxInclusive.getValue().value);
		}
		XSFacet minInclusive = simpleType.getFacet("minInclusive");
		if (minInclusive != null && isNumber(field) && isValidValue(minInclusive)
				&& !hasAnnotation(field, DecimalMin.class)) {
			System.out.println("@DecimalMin(" + minInclusive.getValue().value + "): " + campo + " added to class "
					+ clase);
			field.annotate(DecimalMin.class).param("value", minInclusive.getValue().value);
		}

		XSFacet maxExclusive = simpleType.getFacet("maxExclusive");
		if (maxExclusive != null && isNumber(field) && isValidValue(maxExclusive)
				&& !hasAnnotation(field, DecimalMax.class)) {
			System.out.println("@DecimalMax(" + maxExclusive.getValue().value + "): " + campo + " added to class "
					+ clase);
			JAnnotationUse annotate = field.annotate(DecimalMax.class);
			annotate.param("value", maxExclusive.getValue().value);
			if (jsr349) {
				annotate.param("inclusive", false);
			}
		}
		XSFacet minExclusive = simpleType.getFacet("minExclusive");
		if (minExclusive != null && isNumber(field) && isValidValue(minExclusive)
				&& !hasAnnotation(field, DecimalMin.class)) {
			System.out.println("@DecimalMin(" + minExclusive.getValue().value + "): " + campo + " added to class "
					+ clase);
			JAnnotationUse annotate = field.annotate(DecimalMin.class);
			annotate.param("value", minExclusive.getValue().value);
			if (jsr349) {
				annotate.param("inclusive", false);
			}
		}

		if (simpleType.getFacet("totalDigits") != null) {
			Integer totalDigits = simpleType.getFacet("totalDigits") == null ? null : parseInt(simpleType.getFacet(
					"totalDigits").getValue().value);
			int fractionDigits = simpleType.getFacet("fractionDigits") == null ? 0 : parseInt(simpleType.getFacet(
					"fractionDigits").getValue().value);
			if (!hasAnnotation(field, Digits.class)) {
				System.out.println("@Digits(" + totalDigits + "," + fractionDigits + "): " + campo + " added to class "
						+ clase);
				JAnnotationUse annox = field.annotate(Digits.class).param("integer", (totalDigits - fractionDigits));
				if (simpleType.getFacet("fractionDigits") != null) {
					annox.param("fraction", fractionDigits);
				}
			}
		}
		/**
		 * <annox:annotate annox:class="javax.validation.constraints.Pattern"
		 * message="Name can only contain capital letters, numbers and the symbols '-', '_', '/', ' '"
		 * regexp="^[A-Z0-9_\s//-]*" />
		 */
		if (simpleType.getFacet("pattern") != null) {
			String pattern = simpleType.getFacet("pattern").getValue().value;
			if ("String".equals(field.type().name())) {
				// cxf-codegen fix
				if (!"\\c+".equals(pattern)) {
					System.out.println("@Pattern(" + pattern + "): " + campo + " added to class " + clase);
					if (!hasAnnotation(field, Pattern.class)) {
						field.annotate(Pattern.class).param("regexp", pattern);
					}
				}
			}
		}
	}

	private boolean isNumber(JFieldVar field) {
		for (String type : DECIMAL) {
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

	protected boolean isValidValue(XSFacet facet) {
		String value = facet.getValue().value;
		// cxf-codegen puts max and min as value when there is not anything defined in wsdl.
		return value != null && !isMax(value) && !isMin(value);
	}

	protected boolean isMin(String value) {
		return equals(value, -9223372036854775808L) || equals(value, -2147483648L);
	}

	protected boolean isMax(String value) {
		return equals(value, 9223372036854775807L) || equals(value, 2147483647L);
	}

	protected boolean equals(String value, long val) {
		return value.equals(BigInteger.valueOf(val).toString());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean hasAnnotation(JFieldVar var, Class anotacion) {
		List<JAnnotationUse> lista = (List<JAnnotationUse>) getField("annotations", var);
		if (lista != null) {
			for (JAnnotationUse uso : lista) {
				if (((Class) getField("clazz._class", uso)).getCanonicalName().equals(anotacion.getCanonicalName())) {
					return true;
				}
			}
		}
		return false;
	}

	protected Integer parseInt(String valor) {
		try {

			Integer i = Integer.parseInt(valor);
			if (i < 2147483647 && i > -2147483648) {
				return i;
			}
		} catch (Exception e) {
			try {
				return (int) Math.round(Double.parseDouble(valor));

			} catch (Exception ex) {
				;
			}

		}
		return null;

	}

	/*
	 * protected Long parseLong(String valor) { try { Long i = Long.parseLong(valor); if (i < 2147483647 && i >
	 * -2147483648) { return i; } } catch (Exception e) { return Math.round(Double.parseDouble(valor)); } return null;
	 * 
	 * }
	 */
	protected Object getField(String path, Object oo) {
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

	protected static Field getSimpleField(String fieldName, Class<?> clazz) {
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
}
