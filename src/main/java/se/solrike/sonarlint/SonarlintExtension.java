package se.solrike.sonarlint;

import java.util.Map;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Nested;

/**
 * @author Lucas Persson
 */
public interface SonarlintExtension {

  /**
   * List of rules to exclude from the analysis. E.g 'java:S1186'.
   *
   * @return list of rules.
   */
  SetProperty<String> getExcludeRules();

  /**
   * List of rules to include from the analysis. E.g 'java:S1186'.
   *
   * @return list of rules.
   */
  SetProperty<String> getIncludeRules();

  /**
   * Whether or not this task will ignore failures and continue running the build.
   *
   * @return true if failures should be ignored
   */
  Property<Boolean> getIgnoreFailures();

  /**
   * The maximum number of issues that are tolerated before breaking the build. Defaults to
   * <code>0</code>.
   *
   * @return max number of issues without fail the build.
   */
  Property<Integer> getMaxIssues();

  /**
   * The default directory where reports will be generated.
   */
  DirectoryProperty getReportsDir();

  /**
   * Map of rule parameters for customizing the rules. E.g. regex for parameter names. The key
   * is the rule name. In the inner map the key is the parameter name, e.g. 'Exclude'. Note the
   * parameter names are case sensitive.
   *
   * @return the map of rules
   */
  MapProperty<String, Map<String, String>> getRuleParameters();

  /**
   * Whether issues are to be displayed on the console. Defaults to <code>true</code>.
   *
   * @return true if issues shall be displayed
   */
  Property<Boolean> getShowIssues();

  /**
   * Nested sub tree DSL with report settings.
   *
   * @return the reports
   */
  @Nested
  NamedDomainObjectContainer<SonarlintReport> getReports();

  // not needed in Gradle 7.4 at least
  // default void reports(Action<? super NamedDomainObjectContainer<SonarlintReport>> action) {
  // action.execute(getReports());
  // }

}
