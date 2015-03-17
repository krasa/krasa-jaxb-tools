package com.sun.tools.xjc.addon.krasa;

import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.io.StringWriter;
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
                + "    :   Replaces primitive types of fields and methods by proper Class, WARNING: must be defined before XhashCode or Xequals.  \n";
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
        for (ClassOutline co : outline.getClasses()) {
            HashMap<String, Class> hashMap = new HashMap<String, Class>();
            hashMap.put("int", Integer.class);
            hashMap.put("long", Long.class);
            hashMap.put("boolean", Boolean.class);
            hashMap.put("double", Double.class);
            hashMap.put("float", Float.class);
            hashMap.put("byte", Byte.class);
            hashMap.put("short", Short.class);
            Map<String, JFieldVar> fields = co.implClass.fields();

            for (Map.Entry<String, JFieldVar> stringJFieldVarEntry : fields.entrySet()) {
                JFieldVar fieldVar = stringJFieldVarEntry.getValue();
                JType type = fieldVar.type();
                if (type.isPrimitive()) {
                    Class o = hashMap.get(type.name());
                    if (o != null) {
                        JCodeModel jCodeModel = new JCodeModel();
                        JClass newType = jCodeModel.ref(o);
                        fieldVar.type(newType);
                        setReturnType(newType, getMethodsMap(MethodType.GETTER, fieldVar, co));
                        setParameter(newType, getMethodsMap(MethodType.SETTER, fieldVar, co));
                    }
                }
            }
        }
        return true;
    }

    enum MethodType {
        GETTER, SETTER
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

    /**
     * I hate this shit
     */
    private JMethod getMethodsMap(MethodType type, JFieldVar field, ClassOutline co) {
        String getterBody = "return " + field.name() + ";";
        for (JMethod method : co.implClass.methods()) {
            String name = method.name();
            if (method.type().isPrimitive()) {
                if (MethodType.GETTER == type && (name.startsWith("is") || name.startsWith("get"))) {
                    JStatement o = (JStatement) method.body().getContents().get(0);
                    String s = getterBody(o);
                    if (s.trim().equals(getterBody)) {
                        return method;
                    }
                } else if (MethodType.SETTER == type && name.startsWith("set")) {
                    JStatement o = (JStatement) method.body().getContents().get(0);
                    String s = setterBody(o);
                    if (s.startsWith("this." + field.name() + " =")) {
                        return method;
                    }
                }
            }

        }
        throw new RuntimeException("Failed to find " + type + " for " + field.name() + ", disable XReplacePrimitives and report a bug");
    }

    public static String getterBody(JStatement jStatement) {
        StringWriter w = new StringWriter();
        jStatement.state(new JFormatter(w));
        return w.toString();
    }

    public static String setterBody(JStatement jStatement) {
        StringWriter w = new StringWriter();
        jStatement.state(new JFormatter(w));
        return w.toString();
    }
}
