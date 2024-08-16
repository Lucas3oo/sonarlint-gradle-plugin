package se.solrike.sonarlint.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.SetProperty;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.PluginDetails;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration.Builder;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.commons.RuleKey;
import org.sonarsource.sonarlint.core.commons.Version;
import org.sonarsource.sonarlint.core.plugin.commons.SkipReason;

import se.solrike.sonarlint.Sonarlint;
import se.solrike.sonarlint.impl.util.NodePluginUtil;

/**
 * @author Lucas Persson
 */
public class SonarlintAction {

  private Path mNodeExec;
  private String mNodeVersion;

  public SonarlintAction(Sonarlint task) {
    Project project = task.getProject();
    if (project.getExtensions().findByName("node") != null) {
      NodePluginUtil nodeUtil = new NodePluginUtil();
      if (nodeUtil.getDownload(project)) {
        // this means that the node plugin has been configured with download=true
        mNodeExec = nodeUtil.getNodeExec(project);
        mNodeVersion = nodeUtil.getNodeVersion(project);
      }
    }
  }

  /**
   * Execute the task by calling to SonarLint engine. And generate reports.
   *
   * @param task
   *          - the gradle task
   *
   * @return list of sonarlint issues
   */
  public List<IssueEx> run(Sonarlint task, SetProperty<File> plugins, ProjectLayout layout) {
    return analyze(task, task.getLogger(), plugins, layout);
  }

  @SuppressWarnings({ "java:S1874", "deprecation" })
  protected List<IssueEx> analyze(Sonarlint task, Logger logger, SetProperty<File> plugins, ProjectLayout layout) {
	Map<String, String> sonarProperties = new HashMap<>();

    Project project = task.getProject();
    // Java sourceCompatibility needs to be read so project is actually configured
    if (project.getProperties().containsKey("sourceCompatibility")) {
      String sourceCompatibility = project.getProperties().get("sourceCompatibility").toString();
      sonarProperties.put("sonar.java.source", sourceCompatibility);
    }

    Set<File> compileClasspath = Collections.emptySet();
    if (task.getCompileClasspath() != null) {
      compileClasspath = task.getCompileClasspath().getFiles();
    }
    Set<File> classFiles = Collections.emptySet();
    if (task.getClassFiles() != null) {
      classFiles = task.getClassFiles().getFiles();
    }
    String libs = compileClasspath.stream().filter(File::exists).map(File::getPath).collect(Collectors.joining(","));
    sonarProperties.put("sonar.java.libraries", libs);
    String binaries = classFiles.stream().filter(File::exists).map(File::getPath).collect(Collectors.joining(","));
    sonarProperties.put("sonar.java.binaries", binaries);
    boolean isTestSource = task.getIsTestSource().getOrElse(Boolean.FALSE);

    if (isTestSource) {
      sonarProperties.put("sonar.java.test.libraries", libs);
      sonarProperties.put("sonar.java.test.binaries", binaries);
    }

    Set<File> sourceFiles = task.getSource().getFiles();
    Set<String> excludeRules = task.getExcludeRules().get();
    Set<String> includeRules = task.getIncludeRules().get();
    Map<String, Map<String, String>> ruleParameters = task.getRuleParameters().get();

    Path[] pluginPaths = plugins.get().stream().map(File::toPath).toArray(Path[]::new);
    Path projectDir = layout.getProjectDirectory().getAsFile().toPath();
    Builder builder = StandaloneGlobalConfiguration.builder()
        .addEnabledLanguages(Language.values())
        .addPlugins(pluginPaths)
        .setLogOutput(new GradleClientLogOutput(logger))
        .setWorkDir(layout.getBuildDirectory().getAsFile().get().toPath().resolve("sonarlint"))
        .setSonarLintUserHome(projectDir);

    if (mNodeExec != null && mNodeVersion != null) {
      builder.setNodeJs(mNodeExec, Version.create(mNodeVersion));
    }

    List<ClientInputFileImpl> fileList = sourceFiles.stream()
        .map(f -> new ClientInputFileImpl(projectDir, f.toPath(), isTestSource, StandardCharsets.UTF_8))
        .collect(Collectors.toList());

    StandaloneAnalysisConfiguration analysisConfiguration = StandaloneAnalysisConfiguration.builder()
        .setBaseDir(projectDir)
        .addInputFiles(fileList)
        .addExcludedRules(getRuleKeys(excludeRules))
        .addIncludedRules(getRuleKeys(includeRules))
        .addRuleParameters(getRuleParameters(ruleParameters))
        .putAllExtraProperties(sonarProperties)
        .build();

    StandaloneGlobalConfiguration globalConfiguration = builder.build();
    StandaloneSonarLintEngine engine = new StandaloneSonarLintEngineImpl(globalConfiguration);
    // check for skipped plugins
    Collection<PluginDetails> pluginDetails = engine.getPluginDetails();
    pluginDetails.forEach(details -> {
      if (details.skipReason().isPresent()) {
        String errorMessage = "Failed to load plugin '" + details.name() + "' version " + details.version() + ". ";
        if (details.skipReason().get().equals(SkipReason.IncompatiblePluginApi.INSTANCE)) {
          errorMessage += "Plugin version too new for Sonarlint.";
        }
        else {
          errorMessage += details.skipReason().get().toString();
        }
        // break the build
        throw new GradleException(errorMessage);
      }
    });

    IssueCollector collector = new IssueCollector();
    AnalysisResults results = engine.analyze(analysisConfiguration, collector, new GradleClientLogOutput(logger),
        new GradleProgressMonitor(logger));

    List<IssueEx> issues = collector.getIssues();
    issues.forEach(i -> i.setRulesDetails(engine.getRuleDetails(i.getRuleKey())));

    logger.debug("Files: {}", results.indexedFileCount());
    logger.debug("Issues: {}", issues);

    try {
      engine.stop();
    }
    catch (Exception e) {
      logger.warn("could not stop the engine");
    }

    return issues;
  }

  protected RuleKey[] getRuleKeys(Set<String> rules) {
    return rules.stream().map(RuleKey::parse).toArray(RuleKey[]::new);
  }

  protected Map<RuleKey, Map<String, String>> getRuleParameters(Map<String, Map<String, String>> ruleParameters) {
    return ruleParameters.entrySet()
        .stream()
        .collect(Collectors.toMap(rp -> RuleKey.parse(rp.getKey()), Entry<String, Map<String, String>>::getValue));
  }

}
