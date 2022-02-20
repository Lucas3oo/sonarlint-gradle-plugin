package se.solrike.sonarlint;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lucas Persson
 */
public class SonarlintPlugin implements Plugin<Project> {

  private final Logger sLogger = LoggerFactory.getLogger(SonarlintPlugin.class);
  private static final GradleVersion SUPPORTED_VERSION = GradleVersion.version("7.0");

  public static final String CONFIG_NAME = "sonarlint";
  public static final String PLUGINS_CONFIG_NAME = "sonarlintPlugins";
  public static final String EXTENSION_NAME = "sonarlint";
  public static final String TASK_NAME = "sonarlint";
  public static final String REPORTS_SUBDIR = "sonarlint";

  @Override
  public void apply(Project project) {
    verifyGradleVersion(GradleVersion.current());
    project.getPluginManager().apply(ReportingBasePlugin.class);
    // default reports directory
    DirectoryProperty reportsBaseDir = project.getExtensions().getByType(ReportingExtension.class).getBaseDirectory();
    SonarlintExtension extension = createExtension(project, reportsBaseDir);
    createConfiguration(project);
    createPluginConfiguration(project);
    createTask(project, extension);
  }

  private void createConfiguration(Project project) {
    project.getConfigurations()
        .create(CONFIG_NAME)
        .setDescription("configuration for the Sonarlint plugin")
        .setVisible(false)
        .setTransitive(true);
  }

  private Configuration createPluginConfiguration(Project project) {
    return project.getConfigurations()
        .create(SonarlintPlugin.PLUGINS_CONFIG_NAME)
        .setDescription("configuration for the external SonarLint plugins")
        .setVisible(false)
        .setTransitive(false);
  }

  protected SonarlintExtension createExtension(Project project, DirectoryProperty reportsBaseDir) {
    SonarlintExtension extension = project.getExtensions().create(EXTENSION_NAME, SonarlintExtension.class);

    extension.getIgnoreFailures().set(Boolean.FALSE);
    extension.getMaxIssues().set(0);
    extension.getShowIssues().set(Boolean.TRUE);

    DirectoryProperty sonarlintReportsDirectory = project.getObjects()
        .directoryProperty()
        .convention(reportsBaseDir.map(d -> d.dir(REPORTS_SUBDIR)));
    extension.getReportsDir().set(sonarlintReportsDirectory);

    return extension;
  }

  protected void configureForSourceSet(final SourceSet sourceSet, Sonarlint task) {
    task.setSource(sourceSet.getAllSource());
    task.setCompileClasspath(sourceSet.getCompileClasspath());
    // list of directories, all output directories (compiled classes, processed resources, etc.)
    task.setClassFiles(sourceSet.getOutput());
    task.setIsTestSource(sourceSet.getName().equals("test"));
  }

  // lazy create the tasks
  protected void createTask(Project project, SonarlintExtension extension) {
    project.getPlugins()
        .withType(JavaBasePlugin.class)
        .configureEach(javaBasePlugin -> getSourceSetContainer(project).all(sourceSet -> {
          String name = sourceSet.getTaskName(TASK_NAME, null);

          sLogger.debug("Creating sonarlint task for {}", sourceSet);

          TaskProvider<Sonarlint> taskProvider = project.getTasks().register(name, Sonarlint.class, task -> {
            String description = String.format("Run SonarLint analysis for the source set '%s'", sourceSet.getName());
            task.setDescription(description);
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            // let the task depend on all java compile tasks since sonarlint also needs classes
            // for its analysis
            task.dependsOn(sourceSet.getClassesTaskName());
            configureForSourceSet(sourceSet, task);

            task.getExcludeRules().set(extension.getExcludeRules());
            task.getIncludeRules().set(extension.getIncludeRules());
            task.getMaxIssues().set(extension.getMaxIssues());
            task.getIgnoreFailures().set(extension.getIgnoreFailures());
            task.getRuleParameters().set(extension.getRuleParameters());
            task.getShowIssues().set(extension.getShowIssues());
            task.getReportsDir().set(extension.getReportsDir());
            task.getReports().addAll(extension.getReports().getAsMap().values());

          });

          // let "check" task depend on sonarlint so it gets run automatically
          project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(t -> {
            t.dependsOn(taskProvider);
          });

        }));

  }

  private SourceSetContainer getSourceSetContainer(Project project) {
    return project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
  }

  // private SourceSetContainer getSourceSetContainer(Project project) {
  // if (GradleVersion.current().compareTo(GradleVersion.version("7.1")) < 0) {
  // return project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets();
  // }
  // else {
  // return project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
  // }
  // }

  protected void verifyGradleVersion(GradleVersion version) {
    if (version.compareTo(SUPPORTED_VERSION) < 0) {
      String message = String.format("Gradle version %s is unsupported. Please use %s or later.", version,
          SUPPORTED_VERSION);
      throw new IllegalArgumentException(message);
    }
  }

}
