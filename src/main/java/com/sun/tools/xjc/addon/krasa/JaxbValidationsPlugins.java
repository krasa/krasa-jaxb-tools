package com.sun.tools.xjc.addon.krasa;

import static com.sun.tools.xjc.addon.krasa.Utils.toInt;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.sun.tools.xjc.model.CValuePropertyInfo;
import com.sun.xml.xsom.impl.RestrictionSimpleTypeImpl;
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
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
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
	public static final String VERBOSE = PLUGIN_OPTION_NAME + ":verbose";

	protected String namespace = "http://jaxb.dev.java.net/plugin/code-injector";
	public String targetNamespace = "TARGET_NAMESPACE";
	public boolean jsr349 = false;
	public boolean verbose = true;
	public boolean notNullAnnotations = true;

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

		int index_verbose = arg1.indexOf(VERBOSE);
		if (index_verbose > 0) {
			verbose = Boolean.parseBoolean(arg1.substring(index_verbose
					+ VERBOSE.length() + "=".length()));
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
		JFieldVar field = classOutline.implClass.fields().get(propertyName(property));

		// workaround for choices
		boolean required = property.isRequired();
		if (minOccurs < 0 || minOccurs >= 1 && required) {
			if (!hasAnnotation(field, NotNull.class)) {
				if (notNullAnnotations) {
					log("@NotNull: " + propertyName(property) + " added to class "
							+ classOutline.implClass.name());
					field.annotate(NotNull.class);
				}
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

	private void validAnnotation(final XSType elementType, JFieldVar var, final String propertyName,
								 final String className) {
		if (elementType.getTargetNamespace().startsWith(targetNamespace) && elementType.isComplexType()) {
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
			}
		}

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
			log("@DecimalMax(" + maxExclusive.getValue().value + "): " + propertyName
					+ " added to class " + className);
			JAnnotationUse annotate = field.annotate(DecimalMax.class);
			annotate.param("value", maxExclusive.getValue().value);
			if (jsr349) {
				annotate.param("inclusive", false);
			}
		}
		XSFacet minExclusive = simpleType.getFacet("minExclusive");
		if (minExclusive != null && Utils.isNumber(field) && isValidValue(minExclusive)
				&& !hasAnnotation(field, DecimalMin.class)) {
			log("@DecimalMin(" + minExclusive.getValue().value + "): " + propertyName
					+ " added to class " + className);
			JAnnotationUse annotate = field.annotate(DecimalMin.class);
			annotate.param("value", minExclusive.getValue().value);
			if (jsr349) {
				annotate.param("inclusive", false);
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
					log("@Pattern(" + pattern + "): " + propertyName + " added to class " + className);
					if (!hasAnnotation(field, Pattern.class)) {
						field.annotate(Pattern.class).param("regexp", pattern);
					}
				}
			}
		}
	}

	private boolean isSizeAnnotationApplicable(JFieldVar field) {
		return field.type().name().equals("String")|| field.type().isArray() ;
	}

	/*attribute from parent declaration*/
	private void processAttribute(CValuePropertyInfo property, ClassOutline clase, Outline model) {
		FieldOutline field = model.getField(property);
		String propertyName = property.getName(false);
		String className = clase.implClass.name();

		log("Attribute " + propertyName + " added to class " + className);
		XSComponent definition = property.getSchemaComponent();
		RestrictionSimpleTypeImpl particle = (RestrictionSimpleTypeImpl) definition;
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
				if (notNullAnnotations) {
					log("@NotNull: " + propertyName + " added to class " + className);
					var.annotate(NotNull.class);
				}
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
