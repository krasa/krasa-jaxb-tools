package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.jvnet.jaxb2.maven2.AbstractXJC2Mojo;
import org.jvnet.jaxb2.maven2.test.RunXJC2Mojo;

public class ComplextXsdRunPluginTest extends RunXJC2Mojo {

	protected File getGeneratedDirectory() {
		return new File(getBaseDir(), "target/generated-sources/b");
	}

	@Override
	public File getSchemaDirectory() {
		return new File(getBaseDir(), "src/test/resources/b");
	}

	@Override
	protected void configureMojo(AbstractXJC2Mojo mojo) {
		super.configureMojo(mojo);
		mojo.setProject(new MavenProject());
		mojo.setForceRegenerate(true);
		mojo.setExtension(true);

	}

	@Override
	public List<String> getArgs() {
		final List<String> args = new ArrayList<String>(super.getArgs());
		args.add("-XJsr303Annotations");
		args.add("-XJsr303Annotations:targetNamespace=");
		// args.add("-XJsr303Annotations:targetNamespace=a");
		// args.add("-XJsr303Annotations:JSR_349=true");
		return args;
	}
}
