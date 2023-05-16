package se.solrike.sonarlint;

import java.util.List;

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

  private static final Logger sLogger = LoggerFactory.getLogger(SonarlintPlugin.class);
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
    createTasks(project, extension);
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

  protected void configureTaskForJavaSourceSet(final SourceSet sourceSet, Sonarlint task) {
    // get all sources for the source set including any language and resources
    task.setSource(sourceSet.getAllSource());
    task.setCompileClasspath(sourceSet.getCompileClasspath());
    // list of directories, all output directories (compiled classes, processed resources, etc.)
    task.setClassFiles(sourceSet.getOutput());
    // if the source set is "test" or "testFixtures" or any with test in the name consider it as test source
    task.getIsTestSource().set(sourceSet.getName().contains(SourceSet.TEST_SOURCE_SET_NAME));
  }

  // lazy create the tasks
  protected void createTasks(Project project, SonarlintExtension extension) {
    project.getPlugins()
        .withType(JavaBasePlugin.class)
        .configureEach(javaBasePlugin -> getJavaSourceSetContainer(project).all(sourceSet -> {
          String name = sourceSet.getTaskName(TASK_NAME, null);
          sLogger.debug("Creating sonarlint task for {}", sourceSet);
          TaskProvider<Sonarlint> taskProvider = createTask(project, extension, name);
          String description = String.format("Run SonarLint analysis for the source set '%s'", sourceSet.getName());
          taskProvider.get().setDescription(description);
          // let the task depend on all java compile tasks since sonarlint also needs classes
          // for its analysis
          taskProvider.get().dependsOn(sourceSet.getClassesTaskName());
          configureTaskForJavaSourceSet(sourceSet, taskProvider.get());
        }));

    // also create tasks if the node plugin is applied
    if (project.getPluginManager().hasPlugin("com.github.node-gradle.node")) {
      List<String> taskNames = List.of("Main", "Test");
      for (String taskName : taskNames) {
        TaskProvider<Sonarlint> taskProvider = createTask(project, extension, TASK_NAME + "Node" + taskName);
        String description = String.format("Run SonarLint analysis for node %s classes", taskName.toLowerCase());
        taskProvider.get().setDescription(description);
      }
    }
  }

  protected TaskProvider<Sonarlint> createTask(Project project, SonarlintExtension extension, String taskName) {
    TaskProvider<Sonarlint> taskProvider = project.getTasks().register(taskName, Sonarlint.class, task -> {
      task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);

      task.getExcludeRules().set(extension.getExcludeRules());
      task.getIncludeRules().set(extension.getIncludeRules());
      task.getMaxIssues().set(extension.getMaxIssues());
      task.getIgnoreFailures().set(extension.getIgnoreFailures());
      task.getRuleParameters().set(extension.getRuleParameters());
      task.getShowIssues().set(extension.getShowIssues());
      task.getReportsDir().set(extension.getReportsDir());
      task.getReports().addAll(extension.getReports().getAsMap().values());
      task.getExcludePackages().addAll(extension.getExcludePackages());

    });

    // let "check" task depend on sonarlint so it gets run automatically
    project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(t -> t.dependsOn(taskProvider));

    return taskProvider;
  }

  private SourceSetContainer getJavaSourceSetContainer(Project project) {
    return project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
  }

  protected void verifyGradleVersion(GradleVersion version) {
    if (version.compareTo(SUPPORTED_VERSION) < 0) {
      String message = String.format("Gradle version %s is unsupported. Please use %s or later.", version,
          SUPPORTED_VERSION);
      throw new IllegalArgumentException(message);
    }
  }

}
