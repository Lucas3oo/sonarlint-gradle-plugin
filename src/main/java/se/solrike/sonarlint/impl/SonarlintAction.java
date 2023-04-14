package se.solrike.sonarlint.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration.Builder;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.commons.Version;

import se.solrike.sonarlint.Sonarlint;
import se.solrike.sonarlint.SonarlintPlugin;

/**
 * @author Lucas Persson
 */
public class SonarlintAction {

  /**
   * Execute the task by calling to SonarLint engine. And generate reports.
   *
   * @param task
   *          - the gradle task
   *
   * @return list of sonarlint issues
   */
  public List<IssueEx> run(Sonarlint task) {

    Logger logger = task.getLogger();

    return analyze(task, logger);

  }

  protected List<IssueEx> analyze(Sonarlint task, Logger logger) {

    Project project = task.getProject();
    Set<File> compileClasspath = Collections.emptySet();
    if (task.getCompileClasspath() != null) {
      compileClasspath = task.getCompileClasspath().getFiles();
    }
    Set<File> classFiles = Collections.emptySet();
    if (task.getClassFiles() != null) {
      classFiles = task.getClassFiles().getFiles();
    }
    Set<File> sourceFiles = task.getSource().getFiles();
    boolean isTestSource = task.getIsTestSource().getOrElse(Boolean.FALSE);
    Set<String> excludeRules = task.getExcludeRules().get();
    Set<String> includeRules = task.getIncludeRules().get();
    Map<String, Map<String, String>> ruleParameters = task.getRuleParameters().get();

    Map<String, String> sonarProperties = new HashMap<>(1);
    String libs = compileClasspath.stream().filter(File::exists).map(File::getPath).collect(Collectors.joining(","));
    sonarProperties.put("sonar.java.libraries", libs);
    String binaries = classFiles.stream().filter(File::exists).map(File::getPath).collect(Collectors.joining(","));
    sonarProperties.put("sonar.java.binaries", binaries);
    Configuration pluginConfiguration = project.getConfigurations().getByName(SonarlintPlugin.PLUGINS_CONFIG_NAME);
    Path[] plugins = pluginConfiguration.getFiles().stream().map(File::toPath).toArray(Path[]::new);

    if (isTestSource) {
      sonarProperties.put("sonar.java.test.libraries", libs);
      sonarProperties.put("sonar.java.test.binaries", binaries);
    }

    // Java sourceCompatibility
    if (project.getProperties().containsKey("sourceCompatibility")) {
      String sourceCompatibility = project.getProperties().get("sourceCompatibility").toString();
      sonarProperties.put("sonar.java.source", sourceCompatibility);
    }

    Builder builder = StandaloneGlobalConfiguration.builder()
        .addEnabledLanguages(Language.values())
        .addPlugins(plugins)
        .setLogOutput(new GradleClientLogOutput(logger))
        .setWorkDir(project.getBuildDir().toPath().resolve("sonarlint"))
        .setSonarLintUserHome(project.getProjectDir().toPath());

    if (project.getExtensions().findByName("node") != null) {
      NodePluginUtil nodeUtil = new NodePluginUtil();
      if (nodeUtil.getDownload(project)) {
        // this means that the node plugin has been configured with download=true
        Path nodeExec = nodeUtil.getNodeExec(project);
        logger.debug("node exec: {}", nodeExec);
        builder.setNodeJs(nodeExec, Version.create(nodeUtil.getNodeVersion(project)));
      }
      else {
        logger.error("Node plugin 'com.github.node-gradle.node' is not configured with download=true."
            + " Sonarlint analysis will not be performed on JavaScript/TypeScript source code");
      }
    }

    Path projectDir = project.getProjectDir().toPath();
    List<ClientInputFileImpl> fileList = sourceFiles.stream()
        .map(f -> new ClientInputFileImpl(projectDir, f.toPath(), isTestSource, StandardCharsets.UTF_8))
        .collect(Collectors.toList());

    StandaloneAnalysisConfiguration analysisConfiguration = StandaloneAnalysisConfiguration.builder()
        .setBaseDir(project.getProjectDir().toPath())
        .addInputFiles(fileList)
        .addExcludedRules(getRuleKeys(excludeRules))
        .addIncludedRules(getRuleKeys(includeRules))
        .addRuleParameters(getRuleParameters(ruleParameters))
        .putAllExtraProperties(sonarProperties)
        .build();

    StandaloneGlobalConfiguration globalConfiguration = builder.build();
    StandaloneSonarLintEngine engine = new StandaloneSonarLintEngineImpl(globalConfiguration);
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
