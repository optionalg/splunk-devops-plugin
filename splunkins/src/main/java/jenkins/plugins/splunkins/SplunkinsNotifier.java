package jenkins.plugins.splunkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.io.IOException;
import java.io.PrintStream;
import java.io.FileNotFoundException;

import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by djenkins on 6/18/15.
 */
public class SplunkinsNotifier extends Notifier {
    public boolean collectBuildLog;
    public boolean collectEnvVars;
    public String testArtifactFilename;
    private final static Logger LOGGER = Logger.getLogger(SplunkinsNotifier.class.getName());

    @DataBoundConstructor
    public SplunkinsNotifier(boolean collectBuildLog, boolean collectEnvVars, String testArtifactFilename){
        this.collectBuildLog = collectBuildLog;
        this.collectEnvVars = collectEnvVars;
        this.testArtifactFilename = testArtifactFilename;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        PrintStream buildLogStream = listener.getLogger();

        LOGGER.info("Collect buildlog: "+this.collectBuildLog);
        LOGGER.info("collect artifacts: "+this.collectEnvVars);


        if (this.collectEnvVars) {
            String log = getBuildLog(build);
            LOGGER.info(log);
        }
        if (this.collectEnvVars){
            String envVars = getBuildEnvVars(build, listener);
            LOGGER.info(envVars);
        }
        if (!this.testArtifactFilename.equals("")) {
            String artifactContents = readTestArtifact(testArtifactFilename, build, buildLogStream);
            LOGGER.info("XML report:\n" + artifactContents);
        }

        return true;
    }

    // Returns the build log as a list of strings.
    public String getBuildLog(AbstractBuild<?, ?> build){
        List<String> log = new ArrayList<String>();
        try {
            log = build.getLog(Integer.MAX_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return log.toString();
    }

    // Returns environment variables for the build.
    public String getBuildEnvVars(AbstractBuild<?, ?> build, BuildListener listener){
        EnvVars envVars = null;
        try {
            envVars = build.getEnvironment(listener);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return envVars.toString();
    }

    // Reads test artifact text files and returns their contents. Logs errors to both the Jenkins build log and the
    // Jenkins internal logging.
    public String readTestArtifact(String artifactName, AbstractBuild<?, ?> build, PrintStream buildLogStream){
        String report = "";
        FilePath workspacePath = build.getWorkspace();   // collect junit xml file
        FilePath fullReportPath = new FilePath(workspacePath, artifactName);
        try {
            report = fullReportPath.readToString();  // Attempt to read test artifact
        } catch(FileNotFoundException e ){           // If the test artifact file is not found...
            String noSuchFileMsg = "Build: "+build.getFullDisplayName()+", Splunkins Error: "+e.getMessage();
            LOGGER.warning(noSuchFileMsg);           // Write to Jenkins log
            try {
                // Attempt to write to build's console log
                String buildConsoleError = "Splunkins cannot find JUnit XML Report:" + e.getMessage() + "\n";
                buildLogStream.write(buildConsoleError.getBytes());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            buildLogStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert report != null;
        return report;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
            return true;
        }

        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }
}