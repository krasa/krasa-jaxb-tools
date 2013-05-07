package tests;

import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.parser.XSOMParser;
import org.apache.maven.project.MavenProject;
import org.jvnet.jaxb2.maven2.AbstractXJC2Mojo;
import org.jvnet.jaxb2.maven2.test.RunXJC2Mojo;
import org.xml.sax.SAXException;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RunChoicesPluginTest extends RunXJC2Mojo {

	@Override
	public File getSchemaDirectory() {
		return new File(getBaseDir(), "src/test/resources/choices");
	}


	@Override
	public List<String> getArgs() {
		final List<String> args = new ArrayList<String>(super.getArgs());
		args.add("-XJsr303Annotations");
		args.add("-XJsr303Annotations:targetNamespace=a");
		// args.add("-XJsr303Annotations:JSR_349=true");
		return args;
	}

	protected void configureMojo(final AbstractXJC2Mojo mojo) {
		mojo.setProject(new MavenProject());
		mojo.setForceRegenerate(true);
		mojo.setExtension(true);
		mojo.setSchemaDirectory(getSchemaDirectory());
		mojo.setGenerateDirectory(getGeneratedDirectory());
		mojo.setGeneratePackage("choices");
		mojo.setArgs(getArgs());
		mojo.setVerbose(true);
		mojo.setDebug(false);
		mojo.setWriteCode(isWriteCode());
	}


}
