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
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIDeclaration;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BIXPluginCustomization;
import com.sun.tools.xjc.reader.xmlschema.bindinfo.BindInfo;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.impl.AttributeDeclImpl;
import com.sun.xml.xsom.impl.AttributeUseImpl;
import com.sun.xml.xsom.impl.ComponentImpl;
import com.sun.xml.xsom.impl.ElementDecl;
import com.sun.xml.xsom.impl.ParticleImpl;
import com.sun.xml.xsom.impl.RestrictionSimpleTypeImpl;
import com.sun.xml.xsom.impl.parser.DelayedRef;
import org.xml.sax.ErrorHandler;

import javax.persistence.Column;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sun.tools.xjc.addon.krasa.Utils.toInt;

/**
 * big thanks to original author: cocorossello
 */
public class JaxbValidationsPlugins extends Plugin {

    public static enum GlobalMessageCustomization {
       VALUE,
       ELEMENT,
       ATTRIBUTE
    }
    public static enum MessageTransformation {
       DEFAULT,
       LOWER,
       UPPER,
       CAMEL
    }
    public static class GlobalMessageCustomizationData {
        public GlobalMessageCustomizationData(MessageTransformation messageTransformation, String pattern) {
            super();
            this.messageTransformation = messageTransformation;
            this.pattern = pattern;
        }
        public MessageTransformation getMessageTransformation() {
            return messageTransformation;
        }
        public String getPattern() {
            return pattern;
        }
        private final MessageTransformation messageTransformation;
        private final String pattern;
    }
    private Map<GlobalMessageCustomization, GlobalMessageCustomizationData> globalMessageCustomizationData = null;

	public static final String PLUGIN_OPTION_NAME = "XJsr303Annotations";
	public static final String TARGET_NAMESPACE_PARAMETER_NAME = PLUGIN_OPTION_NAME + ":targetNamespace";
	public static final String JSR_349 = PLUGIN_OPTION_NAME + ":JSR_349";
	public static final String GENERATE_NOT_NULL_ANNOTATIONS = PLUGIN_OPTION_NAME + ":generateNotNullAnnotations";
	public static final String NOT_NULL_ANNOTATIONS_CUSTOM_MESSAGES = PLUGIN_OPTION_NAME + ":notNullAnnotationsCustomMessages";
	public static final String VERBOSE = PLUGIN_OPTION_NAME + ":verbose";
	public static final String GENERATE_JPA_ANNOTATIONS = PLUGIN_OPTION_NAME + ":jpa";

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
				} else if (!value.isEmpty() && !value.equalsIgnoreCase("false")) {
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
				processNotNull(particle, property, classOutline, field);
			}
		}
		if (maxOccurs > 1) {
			if (!hasAnnotation(field, Size.class)) {
				log("@Size (" + minOccurs + "," + maxOccurs + ") " + propertyName(property)
						+ " added to class " + classOutline.implClass.name());
				JAnnotationUse jAnnotationUse =
                        field.annotate(Size.class).param("min", minOccurs).param("max", maxOccurs);
                String message = findCustomMessage(particle, classOutline, property, Size.class);
                if(message != null) {
                    jAnnotationUse.param("message", message);
                }
			}
		}
		if (maxOccurs == -1 && minOccurs > 0) { // maxOccurs="unbounded"
			if (!hasAnnotation(field, Size.class)) {
				log("@Size (" + minOccurs + ") " + propertyName(property) + " added to class "
						+ classOutline.implClass.name());
                JAnnotationUse jAnnotationUse =
                        field.annotate(Size.class).param("min", minOccurs);
                String message = findCustomMessage(particle, classOutline, property, Size.class);
                if(message != null) {
                    jAnnotationUse.param("message", message);
                }
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

		validAnnotation(element, clase, property, elementType, var, propertyName, className);

		if (elementType instanceof XSSimpleType) {
			processType(element, clase, property, (XSSimpleType) elementType, var, propertyName, className);
		} else if (elementType.getBaseType() instanceof XSSimpleType) {
			processType(element, clase, property, (XSSimpleType) elementType.getBaseType(), var, propertyName, className);
		}
	}

	private void processNotNull(ComponentImpl component, CPropertyInfo property, ClassOutline co, JFieldVar field) {
		if (notNullAnnotations) {
			log("@NotNull: " + field.name() + " added to class " + co.implClass.name());
            String message = findCustomMessage(component, co, property, NotNull.class);
            JAnnotationUse annotation = field.annotate(NotNull.class);
            if(message == null) {
                if (notNullPrefixClassName) {
                    annotation.param("message", String.format("%s.%s {%s.message}", co.implClass.name(), field.name(), NotNull.class.getName()));
                } else if (notNullPrefixFieldName) {
                    annotation.param("message", String.format("%s {%s.message}", field.name(), NotNull.class.getName()));
                } else if (notNullCustomMessages) {
                    annotation.param("message", String.format("{%s.message}", NotNull.class.getName()));
                } else if (notNullCustomMessage != null) {
                    annotation.param("message", notNullCustomMessage.replace("{ClassName}", co.implClass.name()).replace("{FieldName}", field.name()));
                }
            } else {
                annotation.param("message", message);
            }
        }
    }

	private void validAnnotation(ComponentImpl component, ClassOutline clase, CPropertyInfo property, final XSType elementType, JFieldVar var, final String propertyName,
								 final String className) {
		if ((targetNamespace == null || elementType.getTargetNamespace().startsWith(targetNamespace)) && elementType.isComplexType()) {
			if (!hasAnnotation(var, Valid.class)) {
				log("@Valid: " + propertyName + " added to class " + className);
				var.annotate(Valid.class);
			}
		}
	}

	public void processType(ComponentImpl component, ClassOutline clase, CPropertyInfo property, XSSimpleType simpleType, JFieldVar field, String propertyName, String className) {
		if (!hasAnnotation(field, Size.class) && isSizeAnnotationApplicable(field)) {
			Integer maxLength = simpleType.getFacet("maxLength") == null ? null : Utils.parseInt(simpleType.getFacet(
					"maxLength").getValue().value);
			Integer minLength = simpleType.getFacet("minLength") == null ? null : Utils.parseInt(simpleType.getFacet(
					"minLength").getValue().value);
			Integer length = simpleType.getFacet("length") == null ? null : Utils.parseInt(simpleType.getFacet(
					"length").getValue().value);

            JAnnotationUse jAnnotationUse = null;
			if (maxLength != null && minLength != null) {
				log("@Size(" + minLength + "," + maxLength + "): " + propertyName + " added to class "
						+ className);
                jAnnotationUse = field.annotate(Size.class).param("min", minLength).param("max", maxLength);
			} else if (minLength != null) {
				log("@Size(" + minLength + ", null): " + propertyName + " added to class " + className);
                jAnnotationUse = field.annotate(Size.class).param("min", minLength);
			} else if (maxLength != null) {
				log("@Size(null, " + maxLength + "): " + propertyName + " added to class " + className);
                jAnnotationUse = field.annotate(Size.class).param("max", maxLength);
			} else if (length != null) {
				log("@Size(" + length + "," + length + "): " + propertyName + " added to class "
						+ className);
                jAnnotationUse = field.annotate(Size.class).param("min", length).param("max", length);
			}
            if(jAnnotationUse != null) {
                String message = findCustomMessage(component, clase, property, Size.class);
                if(message != null) {
                    jAnnotationUse.param("message", message);
                }
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
		XSFacet maxInclusive = simpleType.getFacet("maxInclusive");
		if (maxInclusive != null && Utils.isNumber(field) && isValidValue(maxInclusive)
				&& !hasAnnotation(field, DecimalMax.class)) {
			log("@DecimalMax(" + maxInclusive.getValue().value + "): " + propertyName
					+ " added to class " + className);
            JAnnotationUse jAnnotationUse =
                    field.annotate(DecimalMax.class).param("value", maxInclusive.getValue().value);
	                String message = findCustomMessage(component, clase, property, DecimalMax.class);
	                if(message != null) {
	                    jAnnotationUse.param("message", message);
	                }
		}
		XSFacet minInclusive = simpleType.getFacet("minInclusive");
		if (minInclusive != null && Utils.isNumber(field) && isValidValue(minInclusive)
				&& !hasAnnotation(field, DecimalMin.class)) {
			log("@DecimalMin(" + minInclusive.getValue().value + "): " + propertyName
					+ " added to class " + className);
			JAnnotationUse jAnnotationUse =
                    field.annotate(DecimalMin.class).param("value", minInclusive.getValue().value);
            String message = findCustomMessage(component, clase, property, DecimalMin.class);
            if(message != null) {
                jAnnotationUse.param("message", message);
            }
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
            String message = findCustomMessage(component, clase, property, DecimalMax.class);
            if(message != null) {
                annotate.param("message", message);
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
            String message = findCustomMessage(component, clase, property, DecimalMin.class);
            if(message != null) {
                annotate.param("message", message);
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
				annox.param("fraction", fractionDigits);
                String message = findCustomMessage(component, clase, property, Digits.class);
                if(message != null) {
                    annox.param("message", message);
                }
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
			log("@Pattern.List: " + propertyName + " added to class " + className);
			JAnnotationUse patternListAnnotation = field.annotate(Pattern.List.class);
			JAnnotationArrayMember listValue = patternListAnnotation.paramArray("value");

			if ("String".equals(field.type().name())) {
				for (XSFacet xsFacet : patternList) {
					final String value = xsFacet.getValue().value;
					// cxf-codegen fix
                    if (!"\\c+".equals(value)) {
                        JAnnotationUse annox = listValue.annotate(Pattern.class).param("regexp", replaceXmlProprietals(value));
                        String message = findCustomMessage(component, clase, property, Pattern.class);
                        if(message != null) {
                            annox.param("message", message);
                        }
                    }
				}
			}
		} else if (simpleType.getFacet("pattern") != null) {
			String pattern = simpleType.getFacet("pattern").getValue().value;
			if ("String".equals(field.type().name())) {
				// cxf-codegen fix
				if (!"\\c+".equals(pattern)) {
					log("@Pattern(" + pattern + "): " + propertyName + " added to class " + className);
					if (!hasAnnotation(field, Pattern.class)) {
                        JAnnotationUse annox = field.annotate(Pattern.class).param("regexp", replaceXmlProprietals(pattern));
                        String message = findCustomMessage(component, clase, property, Pattern.class);
                        if(message != null) {
                            annox.param("message", message);
                        }
					}
				}
			}
		} else if ("String".equals(field.type().name())) {
			final List<XSFacet> enumerationList = simpleType.getFacets("enumeration");
			if (enumerationList.size() > 1) { // More than one pattern
				log("@Pattern.List: " + propertyName + " added to class " + className);
				final JAnnotationUse patternListAnnotation = field.annotate(Pattern.List.class);
				final JAnnotationArrayMember listValue = patternListAnnotation.paramArray("value");
				for (XSFacet xsFacet : enumerationList) {
					final String value = xsFacet.getValue().value;
					// cxf-codegen fix
					if (!"\\c+".equals(value)) {
                        JAnnotationUse annox = listValue.annotate(Pattern.class).param("regexp", replaceXmlProprietals(value));
                        String message = findCustomMessage(component, clase, property, Pattern.class);
                        if(message != null) {
                            annox.param("message", message);
                        }
					}
				}
			} else if (simpleType.getFacet("enumeration") != null) {
				final String pattern = simpleType.getFacet("enumeration").getValue().value;
				// cxf-codegen fix
				if (!"\\c+".equals(pattern)) {
					log("@Pattern(" + pattern + "): " + propertyName + " added to class " + className);
                    JAnnotationUse annox = field.annotate(Pattern.class).param("regexp", replaceXmlProprietals(pattern));
                    String message = findCustomMessage(component, clase, property, Pattern.class);
                    if(message != null) {
                        annox.param("message", message);
                    }
				}
			}
		}
	}

	private String replaceXmlProprietals(String pattern) {
		return pattern.replace("\\i", "[_:A-Za-z]").replace("\\c", "[-._:A-Za-z0-9]");
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

		validAnnotation(particle, clase, property, type, var, propertyName, className);
		processType(particle, clase, property, type, var, propertyName, className);
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
				processNotNull(particle, property, clase, var);
			}
		}

		validAnnotation(particle, clase, property, type, var, propertyName, className);
		processType(particle, clase, property, type, var, propertyName, className);
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


    private String findCustomMessage(ComponentImpl component, ClassOutline classOutline, CPropertyInfo property, Class annotationClass) {
        XSAnnotation annotation = component.getOwnerSchema().getAnnotation();
        if(annotation != null && globalMessageCustomizationData == null) {
            // lazy load: need null to initialize only once
            globalMessageCustomizationData =  new HashMap<GlobalMessageCustomization, GlobalMessageCustomizationData>();
            BindInfo bindInfo = (BindInfo) annotation.getAnnotation();
            for (int i = 0; i < bindInfo.size(); i++) {
                BIDeclaration declaration = bindInfo.get(i);
                if (declaration instanceof BIXPluginCustomization) {
                    BIXPluginCustomization bixPluginCustomization = (BIXPluginCustomization) declaration;
                    QName qName = bixPluginCustomization.getName();
                    if ("urn:jaxb:krasa".equals(qName.getNamespaceURI()) && "global-message".equals(qName.getLocalPart())) {
                        GlobalMessageCustomization globalMessageCustomization;
                        String forAttribute = null;
                        try {
                            forAttribute = bixPluginCustomization.element.getAttribute("for");
                            globalMessageCustomization = GlobalMessageCustomization.valueOf(forAttribute.toUpperCase());
                        } catch (Throwable t) {
                            if(forAttribute != null) {
                                throw new IllegalArgumentException("invalid \"for\" attribute [" + forAttribute + "] in krasa:global-message element: only element, attribute and type allowed");
                            }
                            throw new IllegalArgumentException("invalid \"for\" attribute for krasa:global-message element: only element, attribute and type allowed");
                        }
                        MessageTransformation messageTransformation;
                        try {
                            messageTransformation = MessageTransformation.valueOf(bixPluginCustomization.element.getAttribute("transform").toUpperCase());
                        } catch (Throwable t) {
                            messageTransformation = MessageTransformation.DEFAULT;
                        }
                        String value = bixPluginCustomization.element.getAttribute("value");
                        if(value != null && value.trim().length() > 0) {
                            globalMessageCustomizationData.put(
                                    globalMessageCustomization,
                                    new GlobalMessageCustomizationData(
                                            messageTransformation,
                                            value.trim()
                                    )
                            );
                        }
                    }
                }
            }
        }
        annotation = null;
        String xsdPropertyName = null, propertyName = null;
        GlobalMessageCustomizationData globalCustomizationData = null;
        if(component instanceof ElementDecl) {
            ElementDecl elementDecl = ElementDecl.class.cast(component);
            propertyName = propertyName(CElementPropertyInfo.class.cast(property));
            xsdPropertyName = elementDecl.getName();
            globalCustomizationData =
                    globalMessageCustomizationData != null ?
                            globalMessageCustomizationData.get(GlobalMessageCustomization.ELEMENT):
                            null;
            annotation = elementDecl.getAnnotation();
        } else if(component instanceof ParticleImpl) {
            ParticleImpl particle = ParticleImpl.class.cast(component);
            XSTerm xsTerm = particle.getTerm();
            if(xsTerm != null) {
                propertyName = propertyName(CElementPropertyInfo.class.cast(property));
                xsdPropertyName = ElementDecl.class.cast(xsTerm).getName();
                globalCustomizationData =
                        globalMessageCustomizationData != null ?
                                globalMessageCustomizationData.get(GlobalMessageCustomization.ELEMENT):
                                null;
                annotation = xsTerm.getAnnotation();
            }
        } else if(component instanceof AttributeUseImpl) {
            AttributeUseImpl attributeUse = AttributeUseImpl.class.cast(component);
            XSAttributeDecl xsAttributeDecl = attributeUse.getDecl();
            if(xsAttributeDecl != null) {
                propertyName = CAttributePropertyInfo.class.cast(property).getName(false);
                xsdPropertyName = AttributeDeclImpl.class.cast(xsAttributeDecl).getName();
                globalCustomizationData =
                        globalMessageCustomizationData != null ?
                                globalMessageCustomizationData.get(GlobalMessageCustomization.ATTRIBUTE):
                                null;
                annotation = xsAttributeDecl.getAnnotation();
            }
        } else if(component instanceof RestrictionSimpleTypeImpl) {
            RestrictionSimpleTypeImpl restrictionSimpleType = RestrictionSimpleTypeImpl.class.cast(component);
            propertyName = CValuePropertyInfo.class.cast(property).getName(false);
            XSType xsType = restrictionSimpleType.getBaseType();
            if(xsType != null) {
                xsdPropertyName = propertyName;
                globalCustomizationData =
                        globalMessageCustomizationData != null ?
                                globalMessageCustomizationData.get(GlobalMessageCustomization.VALUE):
                                null;
                annotation = xsType.getAnnotation();
            }
        }
        String simpleClassName = classOutline.implClass.name(),
                className = classOutline.implClass.fullName(),
                annotationName = annotationClass.getSimpleName();
        if(xsdPropertyName == null) {
            xsdPropertyName = propertyName;
        }
        if (annotation != null) {
            BindInfo bindInfo = (BindInfo) annotation.getAnnotation();
            if (bindInfo != null && bindInfo.size() > 0) {
                for (int i = 0; i < bindInfo.size(); i++) {
                    BIDeclaration declaration = bindInfo.get(i);
                    if (declaration instanceof BIXPluginCustomization) {
                        BIXPluginCustomization bixPluginCustomization = (BIXPluginCustomization) declaration;
                        QName qName = bixPluginCustomization.getName();
                        if ("urn:jaxb:krasa".equals(qName.getNamespaceURI()) && "message".equals(qName.getLocalPart())) {
                            MessageTransformation messageTransformation;
                            try {
                                messageTransformation = MessageTransformation.valueOf(bixPluginCustomization.element.getAttribute("transform").toUpperCase());
                            } catch (Throwable t) {
                                messageTransformation = MessageTransformation.DEFAULT;
                            }
                            String patternString = bixPluginCustomization.element.getAttribute("value");
                            if (patternString != null && patternString.trim().length() > 0) {
                                return buildMessageCustomization(
                                        messageTransformation,
                                        patternString.trim(),
                                        simpleClassName,
                                        className,
                                        propertyName,
                                        xsdPropertyName,
                                        annotationName
                                );
                            }
                        }
                    }
                }
            }
        }
        return buildMessageCustomization(
                globalCustomizationData,
                simpleClassName,
                className,
                propertyName,
                xsdPropertyName,
                annotationName
        );
    }

    private String buildMessageCustomization(
            GlobalMessageCustomizationData globalMessageCustomizationData,
            String simpleClassName,
            String className,
            String propertyName,
            String xsdPropertyName,
            String annotationName
    ) {
        return globalMessageCustomizationData != null ?
                buildMessageCustomization(
                        globalMessageCustomizationData.getMessageTransformation(),
                        globalMessageCustomizationData.getPattern(),
                        simpleClassName,
                        className,
                        propertyName,
                        xsdPropertyName,
                        annotationName
                ): null;
    }
    private String buildMessageCustomization(
            MessageTransformation messageTransformation,
            String pattern,
            String simpleClassName,
            String className,
            String propertyName,
            String xsdPropertyName,
            String annotationName
    ) {
        String message;
        if(xsdPropertyName == null) {
            xsdPropertyName = propertyName;
        }
        if(MessageTransformation.CAMEL.equals(messageTransformation)) {
            message = pattern.replaceAll(
                    "\\$\\{SimpleClassName}",
                    simpleClassName.length() > 1 ?
                            simpleClassName.substring(0, 1).toLowerCase() + simpleClassName.substring(1) :
                            simpleClassName.toLowerCase()
            ).replaceAll(
                    "\\$\\{ClassName}",
                    className
            ).replaceAll(
                    "\\$\\{FieldName}",
                    propertyName.length() > 1 ?
                            propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1) :
                            propertyName.toLowerCase()
            ).replaceAll(
                    "\\$\\{XsdFieldName}",
                    xsdPropertyName.length() > 1 ?
                            xsdPropertyName.substring(0, 1).toLowerCase() + xsdPropertyName.substring(1) :
                            xsdPropertyName.toLowerCase()
            ).replaceAll(
                    "\\$\\{AnnotationName}",
                    annotationName.length() > 1 ?
                            annotationName.substring(0, 1).toLowerCase() + annotationName.substring(1) :
                            annotationName.toLowerCase()
            );
        } else {
            message = pattern.replaceAll(
                    "\\$\\{SimpleClassName}",
                    simpleClassName
            ).replaceAll(
                    "\\$\\{ClassName}",
                    className
            ).replaceAll(
                    "\\$\\{FieldName}",
                    propertyName
            ).replaceAll(
                    "\\$\\{XsdFieldName}",
                    xsdPropertyName
            ).replaceAll(
                    "\\$\\{AnnotationName}",
                    annotationName
            );
            switch (messageTransformation) {
                case LOWER:
                    message = message.toLowerCase();
                    break;
                case UPPER:
                    message = message.toUpperCase();
                    break;
            }
        }
        return message;
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
