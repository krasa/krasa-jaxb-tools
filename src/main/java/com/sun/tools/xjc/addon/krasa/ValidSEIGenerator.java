package com.sun.tools.xjc.addon.krasa;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.validation.Valid;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.generators.SEIGenerator;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.WSDLToJavaProcessor;

public class ValidSEIGenerator extends SEIGenerator {

	private static final String VALID_PARAM = "VALID_PARAM";

	private static final String VALID_RETURN = "VALID_RETURN";

	private boolean validIn = true;

	private boolean validOut = true;

	@Override
	public void generate(ToolContext penv) throws ToolException {
		parseArguments(penv);

		JAnnotation validAnno = new JAnnotation(Valid.class);
		Map<QName, JavaModel> map = CastUtils.cast((Map<?, ?>) penv.get(WSDLToJavaProcessor.MODEL_MAP));
		for (JavaModel javaModel : map.values()) {
			Map<String, JavaInterface> interfaces = javaModel.getInterfaces();

			for (JavaInterface intf : interfaces.values()) {
				intf.addImport(Valid.class.getCanonicalName());
				List<JavaMethod> methods = intf.getMethods();

				for (JavaMethod method : methods) {
					List<JavaParameter> parameters = method.getParameters();
					if (validOut) {
						method.addAnnotation(VALID_RETURN, validAnno);
					}
					for (JavaParameter param : parameters) {
						if (validIn && (param.isIN() || param.isINOUT())) {
							param.addAnnotation(VALID_PARAM, validAnno);
						}
						if (validOut && (param.isOUT() || param.isINOUT())) {
							param.addAnnotation(VALID_RETURN, validAnno);
						}
					}
				}
			}
		}

		super.generate(penv);
	}

	private void parseArguments(ToolContext penv) {
		if (penv.get(ToolConstants.CFG_XJC_ARGS) != null) {
			String[] xjcArgs = (String[]) penv.get(ToolConstants.CFG_XJC_ARGS);
			for (String arg : xjcArgs) {
				String[] parts = arg.split("=");
				if (parts[0].contains(JaxbValidationsPlugins.GENERATE_SERVICE_VALIDATION_ANNOTATIONS)) {
					parseValidationPolicy(parts[1]);
				}
				LOG.log(Level.FINE, "xjc arg:" + arg);
			}
		}
	}

	public void parseValidationPolicy(String policy) {
		if ("in".equalsIgnoreCase(policy)) {
			validOut = false;
		} else if ("out".equalsIgnoreCase(policy)) {
			validIn = false;
		}
	}

	public String getName() {
		return "krasa";
	}
}