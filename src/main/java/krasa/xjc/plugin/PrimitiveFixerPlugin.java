package krasa.xjc.plugin;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PrimitiveFixerPlugin extends Plugin {

	private static final String OPTION_NAME = "XReplacePrimitives";

	@Override
	public String getOptionName() {
		return OPTION_NAME;
	}

	@Override
	public String getUsage() {
		return "-" + OPTION_NAME
				+ "    :   Replaces primitive types of fields and methods by proper Class: int to java.lang.Integer, long to java.lang.Long, boolean to java.lang.Boolean  \n";
	}

	@Override
	public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
		for (ClassOutline co : outline.getClasses()) {
			HashMap<String, Class> hashMap = new HashMap<String, Class>();
			hashMap.put("int", Integer.class);
			hashMap.put("long", Long.class);
			hashMap.put("boolean", Boolean.class);
			Map<String, JFieldVar> fields = co.implClass.fields();
			Map<String, JMethod> getterSetterMap = getMethodsMap(co);

			for (Map.Entry<String, JFieldVar> stringJFieldVarEntry : fields.entrySet()) {
				JFieldVar fieldVar = stringJFieldVarEntry.getValue();
				JType type = fieldVar.type();
				if (type.isPrimitive()) {
					Class o = hashMap.get(type.name());
					if (o != null) {
						JCodeModel jCodeModel = new JCodeModel();
						JClass newType = jCodeModel.ref(o);
						fieldVar.type(newType);
						String name = fieldVar.name().substring(0, 1).toUpperCase() + fieldVar.name().substring(1);
						JMethod jMethod = getterSetterMap.get("get" + name);
						setReturnType(newType, jMethod);
						jMethod = getterSetterMap.get("set" + name);
						setParameter(newType, jMethod);
						jMethod = getterSetterMap.get("is" + name);
						setReturnType(newType, jMethod);
					}
				}
			}
		}
		return true;
	}

	private void setParameter(JClass newType, JMethod jMethod) {
		if (jMethod != null) {
			JVar jVar = jMethod.listParams()[0];
			jVar.type(newType);
		}
	}

	private void setReturnType(JType type, JMethod jMethod) {
		if (jMethod != null) {
			jMethod.type(type);
		}
	}

	private Map<String, JMethod> getMethodsMap(ClassOutline co) {
		Map<String, JMethod> methodsMap = new HashMap<String, JMethod>();
		Collection<JMethod> methods = co.implClass.methods();
		for (JMethod method : methods) {
			String name = method.name();
			if (method.type().isPrimitive() && (name.startsWith("is") || name.startsWith("get") || name.startsWith("set"))) {
				methodsMap.put(name, method);
			}
		}
		return methodsMap;
	}

}
