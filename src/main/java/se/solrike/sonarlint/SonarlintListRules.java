package se.solrike.sonarlint;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.sonarsource.sonarlint.core.StandaloneSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneGlobalConfiguration.Builder;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleParam;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.commons.Version;

import se.solrike.sonarlint.impl.GradleClientLogOutput;
import se.solrike.sonarlint.impl.util.NodePluginUtil;

/**
 * Gradle task to list all configured Sonarlint rules.
 * <p>
 * If the default settings for a rule has been overridden then those values are listed.
 *
 * @author Lucas Persson
 */
public class SonarlintListRules extends DefaultTask {

  protected Set<String> mIncludedRules;
  protected Set<String> mExcludedRules;
  // ruleKey : [paramKey: value]
  protected Map<String, Map<String, String>> mRuleParameters;

  @TaskAction
  @SuppressWarnings({ "java:S1874", "deprecation" })
  public void run() {
    Logger logger = getLogger();

    if (getProject().getExtensions().findByType(SonarlintExtension.class) != null) {
      SonarlintExtension extension = getProject().getExtensions().findByType(SonarlintExtension.class);
      mIncludedRules = extension.getIncludeRules().getOrNull();
      mExcludedRules = extension.getExcludeRules().getOrNull();
      mRuleParameters = extension.getRuleParameters().getOrNull();
    }

    Configuration pluginConfiguration = getProject().getConfigurations().getByName(SonarlintPlugin.PLUGINS_CONFIG_NAME);
    Path[] plugins = pluginConfiguration.getFiles().stream().map(File::toPath).toArray(Path[]::new);

    Builder builder = StandaloneGlobalConfiguration.builder()
        .addEnabledLanguages(Language.values())
        .addPlugins(plugins)
        .setLogOutput(new GradleClientLogOutput(logger))
        .setWorkDir(getProject().getBuildDir().toPath().resolve("sonarlint"))
        .setSonarLintUserHome(getProject().getProjectDir().toPath());

    if (getProject().getExtensions().findByName("node") != null) {
      NodePluginUtil nodeUtil = new NodePluginUtil();
      if (nodeUtil.getDownload(getProject())) {
        // this means that the node plugin has been configured with download=true
        Path nodeExec = nodeUtil.getNodeExec(getProject());
        logger.debug("node exec: {}", nodeExec);
        builder.setNodeJs(nodeExec, Version.create(nodeUtil.getNodeVersion(getProject())));
      }
      else {
        logger.error("Node plugin 'com.github.node-gradle.node' is not configured with download=true."
            + " Sonarlint analysis will not be performed on JavaScript/TypeScript source code");
      }
    }

    StandaloneGlobalConfiguration globalConfiguration = builder.build();
    StandaloneSonarLintEngine engine = new StandaloneSonarLintEngineImpl(globalConfiguration);
    try {
      List<StandaloneRuleDetails> rules = new ArrayList<>(engine.getAllRuleDetails());
      rules.sort(this::compare);
      rules.forEach(this::printRule);
    }
    finally {
      engine.stop();
    }
  }

  int compare(StandaloneRuleDetails rule1, StandaloneRuleDetails rule2) {
    String key1 = rule1.getKey();
    String key2 = rule2.getKey();
    if (rule1.getLanguage().equals(rule2.getLanguage()) && keyHasId(key1) && keyHasId(key2)) {
      return getKeyId(key1) - getKeyId(key2);
    }
    else {
      return key1.compareTo(key2);
    }
  }

  // if the key is like java:S1176 then it has a numeric "id" in it that we can sort on.
  boolean keyHasId(String key) {
    return key.matches(".*:S\\d+");
  }

  // must be on form similar to java:S1176
  int getKeyId(String key) {
    String id = key.substring(key.indexOf(":S") + 2);
    return Integer.parseInt(id);
  }

  void printRule(StandaloneRuleDetails rule) {
    Logger logger = getLogger();
    logger.warn("[{}] {} - {} - {} - {}", (isActive(rule) ? "x" : " "), rule.getKey(), rule.getName(), rule.getTags(),
        rule.getLanguage());
    rule.paramDetails().forEach(param -> logger.warn("    {} : {}", param.key(), getParamValue(rule.getKey(), param)));
  }

  boolean isActive(StandaloneRuleDetails rule) {
    boolean isActive = rule.isActiveByDefault();
    if (isActive && mExcludedRules != null) {
      isActive = !mExcludedRules.contains(rule.getKey());
    }
    else if (!isActive && mIncludedRules != null) {
      isActive = mIncludedRules.contains(rule.getKey());
    }
    return isActive;
  }

  String getParamValue(String ruleKey, StandaloneRuleParam defaultParam) {
    Map<String, String> overrideParams = mRuleParameters.getOrDefault(ruleKey, Map.of("", ""));
    return overrideParams.getOrDefault(defaultParam.key(), defaultParam.defaultValue());
  }

}
