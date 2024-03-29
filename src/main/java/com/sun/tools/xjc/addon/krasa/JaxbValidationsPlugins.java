package com.sun.tools.xjc.addon.krasa;

import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CAttributePropertyInfo;
import com.sun.tools.xjc.model.CElementPropertyInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CValuePropertyInfo;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.*;
import com.sun.xml.xsom.impl.AttributeUseImpl;
import com.sun.xml.xsom.impl.ElementDecl;
import com.sun.xml.xsom.impl.ParticleImpl;
import com.sun.xml.xsom.impl.SimpleTypeImpl;
import com.sun.xml.xsom.impl.parser.DelayedRef;
import org.xml.sax.ErrorHandler;

import javax.persistence.Column;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import static com.sun.tools.xjc.addon.krasa.Utils.toInt;

/**
 * big thanks to original author: cocorossello
 */
public class JaxbValidationsPlugins extends Plugin {
	
	public static final String PLUGIN_OPTION_NAME = "XJsr303Annotations";
	public static final String TARGET_NAMESPACE_PARAMETER_NAME = PLUGIN_OPTION_NAME + ":targetNamespace";
	public static final String JSR_349 = PLUGIN_OPTION_NAME + ":JSR_349";
	public static final String GENERATE_NOT_NULL_ANNOTATIONS = PLUGIN_OPTION_NAME + ":generateNotNullAnnotations";
	public static final String NOT_NULL_ANNOTATIONS_CUSTOM_MESSAGES = PLUGIN_OPTION_NAME + ":notNullAnnotationsCustomMessages";
	public static final String VERBOSE = PLUGIN_OPTION_NAME + ":verbose";
	public static final String GENERATE_JPA_ANNOTATIONS = PLUGIN_OPTION_NAME + ":jpa";
	public static final String GENERATE_SERVICE_VALIDATION_ANNOTATIONS = PLUGIN_OPTION_NAME + ":generateServiceValidationAnnotations";

	protected String namespace = "http://jaxb.dev.java.net/plugin/code-injector";
	public String targetNamespace = null;
	public boolean jsr349 = false;
	public boolean verbose = true;
	public boolean notNullAnnotations = true;
	public boolean notNullCustomMessages;
	public boolean notNullPrefixFieldName;
	public boolean notNullPrefixClassName;
	public String notNullCustomMessage = null;
	public boolean jpaAnnotations = false;
	public String serviceValidationAnnotations = null;

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

		int index_notNullCustomMessages = arg1.indexOf(NOT_NULL_ANNOTATIONS_CUSTOM_MESSAGES);
		if (index_notNullCustomMessages > 0) {
			String value = arg1.substring(index_notNullCustomMessages + NOT_NULL_ANNOTATIONS_CUSTOM_MESSAGES.length() + "=".length()).trim();
			notNullCustomMessages = Boolean.parseBoolean(value);
			if (!notNullCustomMessages) {
				if (value.equalsIgnoreCase("classname")) {
					notNullCustomMessages = notNullPrefixFieldName = notNullPrefixClassName = true;
				} else if (value.equalsIgnoreCase("fieldname")) {
					notNullCustomMessages = notNullPrefixFieldName = true;
				} else if (value.length() != 0 && !value.equalsIgnoreCase("false")) {
					notNullCustomMessage = value;
				}
			}
			consumed++;
		}

		int index_verbose = arg1.indexOf(VERBOSE);
		if (index_verbose > 0) {
			verbose = Boolean.parseBoolean(arg1.substring(index_verbose
					+ VERBOSE.length() + "=".length()));
			consumed++;
		}
		int index_generateJpaAnnotations = arg1.indexOf(GENERATE_JPA_ANNOTATIONS);
		if (index_generateJpaAnnotations > 0) {
			jpaAnnotations = Boolean.parseBoolean(arg1.substring(index_generateJpaAnnotations
					+ GENERATE_JPA_ANNOTATIONS.length() + "=".length()));
			consumed++;
		}

		int index_serviceValidationAnnotation = arg1.indexOf(GENERATE_SERVICE_VALIDATION_ANNOTATIONS);
		if (index_serviceValidationAnnotation > 0) {
			serviceValidationAnnotations = arg1.substring(index_serviceValidationAnnotation
					+ GENERATE_SERVICE_VALIDATION_ANNOTATIONS.length() + "=".length()).trim();
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
				List<CPropertyInfo> properties = co.target.getProperties();
				for (CPropertyInfo property : properties) {
					if (property instanceof CElementPropertyInfo) {
						processElement((CElementPropertyInfo) property, co, model);
					} else if (property instanceof CAttributePropertyInfo) {
						processAttribute((CAttributePropertyInfo) property, co, model);
					} else if (property instanceof CValuePropertyInfo) {
						processAttribute((CValuePropertyInfo) property, co, model);
					}
				}
			}
			return true;
		} catch (Exception e) {
			log(e);
			return false;
		}
	}


	/**
	 * XS:Element
	 */
	public void processElement(CElementPropertyInfo property, ClassOutline classOutline, Outline model) {
		XSComponent schemaComponent = property.getSchemaComponent();
		ParticleImpl particle = (ParticleImpl) schemaComponent;
		// must be reflection because of cxf-codegen
		int maxOccurs = toInt(Utils.getField("maxOccurs", particle));
		int minOccurs = toInt(Utils.getField("minOccurs", particle));
		boolean nillable = toBoolean(Utils.getField("nillable",particle.getTerm())); 
		JFieldVar field = classOutline.implClass.fields().get(propertyName(property));

		// workaround for choices
		boolean required = property.isRequired();
		if (minOccurs < 0 || minOccurs >= 1 && required && !nillable) {
			if (!hasAnnotation(field, NotNull.class)) {
				processNotNull(classOutline, field);
			}
		}
		if (maxOccurs > 1) {
			if (!hasAnnotation(field, Size.class)) {
				log("@Size (" + minOccurs + "," + maxOccurs + ") " + propertyName(property)
						+ " added to class " + classOutline.implClass.name());

				field.annotate(Size.class).param("min", minOccurs).param("max", maxOccurs);
			}
		}
		if (maxOccurs == -1 && minOccurs > 0) { // maxOccurs="unbounded"
			if (!hasAnnotation(field, Size.class)) {
				log("@Size (" + minOccurs + ") " + propertyName(property) + " added to class "
						+ classOutline.implClass.name());
				field.annotate(Size.class).param("min", minOccurs);
			}
		}

		XSTerm term = particle.getTerm();
		if (term instanceof ElementDecl) {
			processElement(property, classOutline, field, (ElementDecl) term);
		} else if (term instanceof DelayedRef.Element) {
			XSElementDecl xsElementDecl = ((DelayedRef.Element) term).get();
			processElement(property, classOutline, field, (ElementDecl) xsElementDecl);
		}

	}

	private boolean toBoolean(Object field) {
		if(field != null){
			return Boolean.parseBoolean(field.toString()); 
		}
		return false;
	}

	private void processElement(CElementPropertyInfo property, ClassOutline clase, JFieldVar var, ElementDecl element) {
		String propertyName = propertyName(property);
		String className = clase.implClass.name();
		XSType elementType = element.getType();

		validAnnotation(elementType, var, propertyName, className);

		if (elementType instanceof XSSimpleType) {
			processType((XSSimpleType) elementType, var, propertyName, className);
		} else if (elementType.getBaseType() instanceof XSSimpleType) {
			processType((XSSimpleType) elementType.getBaseType(), var, propertyName, className);
		}
	}

	private void processNotNull(ClassOutline co, JFieldVar field) {
		if (notNullAnnotations) {
			log("@NotNull: " + field.name() + " added to class " + co.implClass.name());
			JAnnotationUse annotation = field.annotate(NotNull.class);
			if (notNullPrefixClassName) {
				annotation.param("message", String.format("%s.%s {%s.message}", co.implClass.name(), field.name(), NotNull.class.getName()));
			} else if (notNullPrefixFieldName) {
				annotation.param("message", String.format("%s {%s.message}", field.name(), NotNull.class.getName()));
			} else if (notNullCustomMessages) {
				annotation.param("message", String.format("{%s.message}", NotNull.class.getName()));
			} else if (notNullCustomMessage != null) {
				annotation.param("message", notNullCustomMessage.replace("{ClassName}", co.implClass.name()).replace("{FieldName}", field.name()));
			}
		}
	}

	private void validAnnotation(final XSType elementType, JFieldVar var, final String propertyName,
								 final String className) {
		if ((targetNamespace == null || elementType.getTargetNamespace().startsWith(targetNamespace)) &&
                (elementType.isComplexType() || Utils.isCustomType(var))) {
			if (!hasAnnotation(var, Valid.class)) {
				log("@Valid: " + propertyName + " added to class " + className);
				var.annotate(Valid.class);
			}
		}
	}

    public void processType(XSSimpleType simpleType, JFieldVar field, String propertyName, String className) {
		if (!hasAnnotation(field, Size.class) && isSizeAnnotationApplicable(field)) {
			Integer maxLength = simpleType.getFacet("maxLength") == null ? null : Utils.parseInt(simpleType.getFacet(
					"maxLength").getValue().value);
			Integer minLength = simpleType.getFacet("minLength") == null ? null : Utils.parseInt(simpleType.getFacet(
					"minLength").getValue().value);
			Integer length = simpleType.getFacet("length") == null ? null : Utils.parseInt(simpleType.getFacet(
					"length").getValue().value);

			if (maxLength != null && minLength != null) {
				log("@Size(" + minLength + "," + maxLength + "): " + propertyName + " added to class "
						+ className);
				field.annotate(Size.class).param("min", minLength).param("max", maxLength);
			} else if (minLength != null) {
				log("@Size(" + minLength + ", null): " + propertyName + " added to class " + className);
				field.annotate(Size.class).param("min", minLength);
			} else if (maxLength != null) {
				log("@Size(null, " + maxLength + "): " + propertyName + " added to class " + className);
				field.annotate(Size.class).param("max", maxLength);
			} else if (length != null) {
				log("@Size(" + length + "," + length + "): " + propertyName + " added to class "
						+ className);
				field.annotate(Size.class).param("min", length).param("max", length);
			}
		}
		if (jpaAnnotations && isSizeAnnotationApplicable(field)) {
			Integer maxLength = simpleType.getFacet("maxLength") == null ? null : Utils.parseInt(simpleType.getFacet(
					"maxLength").getValue().value);
			if (maxLength != null) {
				log("@Column(null, " + maxLength + "): " + propertyName + " added to class " + className);
				field.annotate(Column.class).param("length", maxLength);
			}
		}
		//TODO minExclusive=0, fractionDigits=2 wrong annotation https://github.com/krasa/krasa-jaxb-tools/issues/38 
		XSFacet maxInclusive = simpleType.getFacet("maxInclusive");
		if (maxInclusive != null && Utils.isNumber(field) && isValidValue(maxInclusive)
				&& !hasAnnotation(field, DecimalMax.class)) {
			log("@DecimalMax(" + maxInclusive.getValue().value + "): " + propertyName
					+ " added to class " + className);
			field.annotate(DecimalMax.class).param("value", maxInclusive.getValue().value);
		}
		XSFacet minInclusive = simpleType.getFacet("minInclusive");
		if (minInclusive != null && Utils.isNumber(field) && isValidValue(minInclusive)
				&& !hasAnnotation(field, DecimalMin.class)) {
			log("@DecimalMin(" + minInclusive.getValue().value + "): " + propertyName
					+ " added to class " + className);
			field.annotate(DecimalMin.class).param("value", minInclusive.getValue().value);
		}

		XSFacet maxExclusive = simpleType.getFacet("maxExclusive");
		if (maxExclusive != null && Utils.isNumber(field) && isValidValue(maxExclusive)
				&& !hasAnnotation(field, DecimalMax.class)) {
			JAnnotationUse annotate = field.annotate(DecimalMax.class);
			if (jsr349) {
				log("@DecimalMax(value = " + maxExclusive.getValue().value + ", inclusive = false): " + propertyName
						+ " added to class " + className);
				annotate.param("value", maxExclusive.getValue().value);
				annotate.param("inclusive", false);
			} else {
				final BigInteger value = new BigInteger(maxExclusive.getValue().value).subtract(BigInteger.ONE);
				log("@DecimalMax(" + value.toString() + "): " + propertyName + " added to class " + className);
				annotate.param("value", value.toString());
			}
		}
		XSFacet minExclusive = simpleType.getFacet("minExclusive");
		if (minExclusive != null && Utils.isNumber(field) && isValidValue(minExclusive)
				&& !hasAnnotation(field, DecimalMin.class)) {
			JAnnotationUse annotate = field.annotate(DecimalMin.class);
			if (jsr349) {
				log("@DecimalMin(value = " + minExclusive.getValue().value + ", inclusive = false): " + propertyName
						+ " added to class " + className);
				annotate.param("value", minExclusive.getValue().value);
				annotate.param("inclusive", false);
			} else {
				final BigInteger value = new BigInteger(minExclusive.getValue().value).add(BigInteger.ONE);
				log("@DecimalMax(" + value.toString() + "): " + propertyName + " added to class " + className);
				annotate.param("value", value.toString());
			}
		}

		if (simpleType.getFacet("totalDigits") != null && Utils.isNumber(field)) {
			Integer totalDigits = simpleType.getFacet("totalDigits") == null ? null
					: Utils.parseInt(simpleType.getFacet("totalDigits").getValue().value);
			int fractionDigits = simpleType.getFacet("fractionDigits") == null ? 0
					: Utils.parseInt(simpleType.getFacet("fractionDigits").getValue().value);
			if (!hasAnnotation(field, Digits.class)) {
				log("@Digits(" + totalDigits + "," + fractionDigits + "): " + propertyName
						+ " added to class " + className);
				JAnnotationUse annox = field.annotate(Digits.class).param("integer", totalDigits);
				annox.param("fraction", fractionDigits);
			}
			if (jpaAnnotations) {
				field.annotate(Column.class).param("precision", totalDigits).param("scale", fractionDigits);
			}
		}
		/**
		 * <annox:annotate annox:class="javax.validation.constraints.Pattern"
		 * message="Name can only contain capital letters, numbers and the symbols '-', '_', '/', ' '"
		 * regexp="^[A-Z0-9_\s//-]*" />
		 */
		List<XSFacet> patternList = simpleType.getFacets("pattern");
		if (patternList.size() > 1) { // More than one pattern
			if ("String".equals(field.type().name())) {
				if (simpleType.getBaseType() instanceof XSSimpleType && ((XSSimpleType) simpleType.getBaseType())
						.getFacet("pattern") != null) {
					log("@Pattern.List: " + propertyName + " added to class " + className);
					JAnnotationUse patternListAnnotation = field.annotate(Pattern.List.class);
					JAnnotationArrayMember listValue = patternListAnnotation.paramArray("value");

					String basePattern = ((XSSimpleType) simpleType.getBaseType()).getFacet("pattern").getValue().value;
					listValue.annotate(Pattern.class).param("regexp", replaceXmlProprietals(basePattern));

					log("@Pattern: " + propertyName + " added to class " + className);
					final JAnnotationUse patternAnnotation = listValue.annotate(Pattern.class);
					annotateMultiplePattern(patternList, patternAnnotation);
				} else {
					log("@Pattern: " + propertyName + " added to class " + className);
					final JAnnotationUse patternAnnotation = field.annotate(Pattern.class);
					annotateMultiplePattern(patternList, patternAnnotation);
				}
			}
		} else if (simpleType.getFacet("pattern") != null) {
			String pattern = simpleType.getFacet("pattern").getValue().value;
			if ("String".equals(field.type().name())) {
				if (simpleType.getBaseType() instanceof XSSimpleType && ((XSSimpleType) simpleType.getBaseType())
						.getFacet("pattern") != null) {
					log("@Pattern.List: " + propertyName + " added to class " + className);
					JAnnotationUse patternListAnnotation = field.annotate(Pattern.List.class);
					JAnnotationArrayMember listValue = patternListAnnotation.paramArray("value");
					String basePattern = ((XSSimpleType) simpleType.getBaseType()).getFacet("pattern").getValue().value;
					listValue.annotate(Pattern.class).param("regexp", replaceXmlProprietals(basePattern));
					// cxf-codegen fix
					if (!"\\c+".equals(pattern)) {
						log("@Pattern(" + pattern + "): " + propertyName + " added to class " + className);
						if (!hasAnnotation(field, Pattern.class)) {
							listValue.annotate(Pattern.class).param("regexp", replaceXmlProprietals(pattern));
						}
					}
				} else {
					// cxf-codegen fix
					if (!"\\c+".equals(pattern)) {
						log("@Pattern(" + pattern + "): " + propertyName + " added to class " + className);
						if (!hasAnnotation(field, Pattern.class)) {
							field.annotate(Pattern.class).param("regexp", replaceXmlProprietals(pattern));
						}
					}
				}
			}
		} else if (field != null && "String".equals(field.type().name())) {
			final List<XSFacet> enumerationList = simpleType.getFacets("enumeration");
			if (enumerationList.size() > 1) { // More than one pattern
				log("@Pattern: " + propertyName + " added to class " + className);
				final JAnnotationUse patternListAnnotation = field.annotate(Pattern.class);
				annotateMultipleEnumerationPattern(enumerationList, patternListAnnotation);
			} else if (simpleType.getFacet("enumeration") != null) {
				final String pattern = simpleType.getFacet("enumeration").getValue().value;
				// cxf-codegen fix
				if (!"\\c+".equals(pattern)) {
					log("@Pattern(" + pattern + "): " + propertyName + " added to class " + className);
					field.annotate(Pattern.class).param("regexp", escapeRegex(replaceXmlProprietals(pattern)));
				}
			}
		}
	}

	private void annotateMultiplePattern(final List<XSFacet> patternList, final JAnnotationUse patternAnnotation) {
		StringBuilder sb = new StringBuilder();
		for (XSFacet xsFacet : patternList) {
			final String value = xsFacet.getValue().value;
			// cxf-codegen fix
			if (!"\\c+".equals(value)) {
				sb.append("(").append(replaceXmlProprietals(value)).append(")|");
			}
		}
		patternAnnotation.param("regexp", sb.substring(0, sb.length() - 1));
	}

	private void annotateMultipleEnumerationPattern(final List<XSFacet> patternList,
													final JAnnotationUse patternAnnotation) {
		StringBuilder sb = new StringBuilder();
		for (XSFacet xsFacet : patternList) {
			final String value = xsFacet.getValue().value;
			// cxf-codegen fix
			if (!"\\c+".equals(value)) {
				sb.append("(").append(escapeRegex(replaceXmlProprietals(value))).append(")|");
			}
		}
		patternAnnotation.param("regexp", sb.substring(0, sb.length() - 1));
	}

	private String replaceXmlProprietals(String pattern) {
		return pattern.replace("\\i", "[_:A-Za-z]").replace("\\c", "[-._:A-Za-z0-9]");
	}

	/*
	 * \Q indicates begin of quoted regex text, \E indicates end of quoted regex text
	 */
	private String escapeRegex(String pattern) {
		return java.util.regex.Pattern.quote(pattern);
	}

	private boolean isSizeAnnotationApplicable(JFieldVar field) {
		if(field == null) {
			return false;
		}
		return field.type().name().equals("String")|| field.type().isArray() ;
	}

	/*attribute from parent declaration*/
	private void processAttribute(CValuePropertyInfo property, ClassOutline clase, Outline model) {
		FieldOutline field = model.getField(property);
		String propertyName = property.getName(false);
		String className = clase.implClass.name();

		log("Attribute " + propertyName + " added to class " + className);
		XSComponent definition = property.getSchemaComponent();
		SimpleTypeImpl particle = (SimpleTypeImpl) definition;
		XSSimpleType type = particle.asSimpleType();
		JFieldVar var = clase.implClass.fields().get(propertyName);


//		if (particle.isRequired()) {
//			if (!hasAnnotation(var, NotNull.class)) {
//				if (notNullAnnotations) {
//					System.out.println("@NotNull: " + propertyName + " added to class " + className);
//					var.annotate(NotNull.class);
//				}
//			}
//		}

		validAnnotation(type, var, propertyName, className);
		processType(type, var, propertyName, className);
	}

	/**
	 * XS:Attribute
	 */
	public void processAttribute(CAttributePropertyInfo property, ClassOutline clase, Outline model) {
		FieldOutline field = model.getField(property);
		String propertyName = property.getName(false);
		String className = clase.implClass.name();

		log("Attribute " + propertyName + " added to class " + className);
		XSComponent definition = property.getSchemaComponent();
		AttributeUseImpl particle = (AttributeUseImpl) definition;
		XSSimpleType type = particle.getDecl().getType();

		JFieldVar var = clase.implClass.fields().get(propertyName);
		if (particle.isRequired()) {
			if (!hasAnnotation(var, NotNull.class)) {
				processNotNull(clase, var);
			}
		}

		validAnnotation(type, var, propertyName, className);
		processType(type, var, propertyName, className);
	}

	protected boolean isValidValue(XSFacet facet) {
		String value = facet.getValue().value;
		// cxf-codegen puts max and min as value when there is not anything defined in wsdl.
		return value != null && !Utils.isMax(value) && !Utils.isMin(value);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public boolean hasAnnotation(JFieldVar var, Class annotationClass) {
		List<JAnnotationUse> list = (List<JAnnotationUse>) Utils.getField("annotations", var);
		if (list != null) {
			for (JAnnotationUse annotationUse : list) {
				if (((Class) Utils.getField("clazz._class", annotationUse)).getCanonicalName().equals(
						annotationClass.getCanonicalName())) {
					return true;
				}
			}
		}
		return false;
	}

	private String propertyName(CElementPropertyInfo property) {
		return property.getName(false);
	}


	private void log(Exception e) {
		e.printStackTrace();
	}

	private void log(String log) {
		if (verbose) {
			System.out.println(log);
		}
	}
}
