package se.solrike.sonarlint;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.CompileClasspath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;

import se.solrike.sonarlint.impl.IssueEx;
import se.solrike.sonarlint.impl.ReportAction;
import se.solrike.sonarlint.impl.SonarlintAction;

/**
 * Gradle task to execute sonarlint stand alone code analysis.
 *
 * @author Lucas Persson
 */
public abstract class Sonarlint extends SourceTask {

  private FileCollection mCompileClasspath;
  private FileCollection mClassFiles;
  private final SonarlintAction mAction;

  /**
   * List of rules to exclude from the analysis. E.g 'java:S1186'.
   *
   * @return list of rules.
   */
  @Input
  public abstract SetProperty<String> getExcludeRules();

  /**
   * List of rules to include from the analysis. E.g 'java:S1186'.
   *
   * @return list of rules.
   */
  @Input
  public abstract SetProperty<String> getIncludeRules();

  /**
   * Whether or not this task will ignore failures and continue running the build.
   *
   * @return true if failures should be ignored
   */
  @Input
  @Optional
  public abstract Property<Boolean> getIgnoreFailures();

  @Input
  @Optional
  public abstract Property<Boolean> getIsTestSource();

  /**
   * The maximum number of issues that are tolerated before breaking the build or setting the failure property.
   *
   * @return the maximum number of issues allowed
   */
  @Input
  @Optional
  public abstract Property<Integer> getMaxIssues();

  /**
   * Whether issues are to be displayed on the console. Defaults to <code>true</code>.
   *
   * @return true if issues shall be displayed
   */
  @Input
  @Optional
  public abstract Property<Boolean> getShowIssues();

  /**
   * Map of reports settings.
   *
   * @return the reports
   */
  // if annotated with @Input then Gradle tries to fingerprint the reports and
  // serialise it which will not work if you specify outputLocation which is a File object.
  @Internal
  public abstract NamedDomainObjectContainer<SonarlintReport> getReports();

  /**
   * The directory where reports will be default generated.
   *
   * @return the directory
   */
  @OutputDirectory
  @Optional
  public abstract DirectoryProperty getReportsDir();

  /**
   * Map of rule parameters for customizing the rules. E.g. regex for parameter names. The key is the rule name. In the
   * inner map the key is the parameter name, e.g. 'Exclude'. Note the parameter names are case sensitive.
   *
   * @return the map of rules
   */
  @Input
  public abstract MapProperty<String, Map<String, String>> getRuleParameters();

  @CompileClasspath
  @Optional
  public FileCollection getCompileClasspath() {
    return mCompileClasspath;
  }

  public void setCompileClasspath(FileCollection fileCollection) {
    mCompileClasspath = fileCollection;
  }

  @Classpath
  @Optional
  public FileCollection getClassFiles() {
    return mClassFiles;
  }

  public void setClassFiles(FileCollection sourceSetOutput) {
    mClassFiles = sourceSetOutput;
  }

  /**
   * The sources for this task are relatively relocatable even though it produces output that includes absolute paths.
   * This is a compromise made to ensure that results can be reused between different builds. The downside is that
   * up-to-date results, or results loaded from cache can show different absolute paths than would be produced if the
   * task was executed.
   */
  @Override
  @PathSensitive(PathSensitivity.RELATIVE)
  public FileTree getSource() {
    return super.getSource();
  }

  @Inject
  protected abstract ProjectLayout getProjectLayout();

  @Inject
  protected abstract ProviderFactory getProjectProvider();

  @Internal
  protected abstract SetProperty<File> getSonarlintConfiguration();

  @SuppressWarnings("java:S5993")
  public Sonarlint() {
    mAction = new SonarlintAction(this);
    Provider<Set<ResolvedArtifactResult>> artifactProvider = getProject().getConfigurations()
        .named(SonarlintPlugin.PLUGINS_CONFIG_NAME)
        .flatMap(c -> c.getIncoming().getArtifacts().getResolvedArtifacts());
    getSonarlintConfiguration()
        .set(artifactProvider.map(p -> p.stream().map(ResolvedArtifactResult::getFile).collect(Collectors.toSet())));
  }

  /**
   * Execute the Sonarlint analysis on the source code.
   */
  @TaskAction
  public void run() {
    Logger logger = getLogger();

    logTaskParameters();

    List<IssueEx> issues = mAction.run(this, getSonarlintConfiguration(), getProjectLayout());

    String resultMessage = String.format("%d SonarLint issue(s) were found. Max issue(s) allowed: %d.", issues.size(),
        getMaxIssues().getOrElse(0));
    logger.error(resultMessage);

    ReportAction reportAction = new ReportAction(this, logger, getProjectLayout(), getProjectProvider());
    reportAction.report(issues);

    // optionally generate console info
    if (Boolean.TRUE.equals(getShowIssues().getOrElse(Boolean.TRUE)) && logger.isErrorEnabled()) {
      for (IssueEx issue : issues) {
        logger.error("\n{} {} {} {} at: {}:{}:{}", reportAction.getIssueTypeIcon(issue.getType()),
            reportAction.getIssueSeverityIcon(issue.getSeverity()), issue.getRuleKey(), issue.getMessage(),
            issue.getInputFileRelativePath(), issue.getStartLine(), issue.getStartLineOffset());
      }
    }

    boolean ignoreFailures = getIgnoreFailures().getOrElse(Boolean.FALSE);
    if ((!ignoreFailures) && issues.size() > getMaxIssues().getOrElse(0)) {
      // fail build
      throw new GradleException(resultMessage);
    }

  }

  private void logTaskParameters() {
    if (getLogger().isDebugEnabled()) {
      getLogger().debug(">>> SonarLint task " + getName());
      getLogger().debug("SonarLint reports " + getReports().getAsMap());
      getLogger().debug("Exclude rules: " + getExcludeRules().getOrNull());
      getLogger().debug("Include rules: " + getIncludeRules().getOrNull());
      getLogger().debug("RuleParams: " + getRuleParameters().getOrNull());
      getLogger().debug("Source: {}", getSource().getAsPath());
      getLogger().debug("Source files: {}", getSource().getAsFileTree().getFiles());
      getLogger().debug("IsTestSource: {}", getIsTestSource().getOrElse(Boolean.FALSE));
      String sonarlintPlugins = getSonarlintConfiguration().get()
          .stream()
          .map(File::getName)
          .reduce("", (p1, p2) -> p1 + ", " + p2);
      getLogger().debug("Plugins: {}", sonarlintPlugins);
      getLogger().debug("<<< SonarLint task " + getName());
    }
  }

}
