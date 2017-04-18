package tests;

import org.apache.maven.project.MavenProject;
import org.jvnet.jaxb2.maven2.AbstractXJC2Mojo;
import org.jvnet.jaxb2.maven2.test.RunXJC2Mojo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MultiplePatternsWithBasePluginTest extends RunXJC2Mojo {

    public static final String STRING = "multiplePatternsWithBase";

    protected File getGeneratedDirectory() {
        return new File(getBaseDir(), "target/generated-sources/" + STRING);
    }

    @Override
    public File getSchemaDirectory() {
        return new File(getBaseDir(), "src/test/resources/" + STRING);
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
        return args;
    }

}