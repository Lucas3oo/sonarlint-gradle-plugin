# sonarlint-gradle-plugin
Gradle plugin for SonarLint code analysis.


## Usage

### Apply to your project

Apply the plugin to your project.
Refer [the Gradle Plugin portal](https://plugins.gradle.org/plugin/se.solrike.sonarlint) about the detail of installation procedure.

Gradle 7.0 or later must be used.

### Configure SonarLint Plugin

Configure `sonarlint` extension to configure the behaviour of tasks:

```groovy
sonarlint {
    excludeRules = ['java:S1186']
    includeRules = ['java:S1696', 'java:S4266']
    ignoreFailures = false
    maxIssues = 0
    reportsDir = 'someFolder' // default build/reports/sonarlint
    // note that rule parameter names are case sensitive
    ruleParameters = [
      'java:S1176' : [
        'forClasses':'**.api.**',      // need javadoc for public methods in package matching 'api'
        'exclusion': '**.internal.**'] // do not need javadoc for classes under 'internal'
    ]
    showIssues = true
}
```

Configure `sonarlintPlugins` to apply any SonarLint plugin:

```groovy
dependencies {
    sonarlintPlugins 'org.sonarsource.html:sonar-html-plugin:3.6.0.3106'
    sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.8.1.28740'
    sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:8.8.0.17228' // both JS and TS
    sonarlintPlugins 'org.sonarsource.typescript:sonar-typescript-plugin:2.1.0.4359'
    sonarlintPlugins 'org.sonarsource.xml:sonar-xml-plugin:2.5.0.3376'
}
```

### Apply to Java project

Apply this plugin with [the `java` plugin](https://docs.gradle.org/current/userguide/java_plugin.html) to your project,
then `Sonarlint` task will be generated for each existing sourceSet. E.g. `sonarlintMain` and `sonarlintTest`



### Configure the SonarLint Task

Configure `Sonarlint` directly, to set task-specific properties.

```groovy
// Example to configure HTML report
sonarlintMain {
    reports {
        text.enabled = false
        html {
            enabled = true
            // default location build/reports/sonarlint/sonarlintMain.html
            outputLocation = layout.buildDirectory.file('my_sonarlint_super_report.html')
        }
    }
}
```
```groovy
// Example to configure different rules etc for the test source
sonarlintTest {
    excludeRules = ['java:S1001']
    includeRules = ['java:S1002', 'java:S1003']
    ignoreFailures = true
}
```


## Sonarlint version mapping

By default, this Gradle Plugin uses the Sonarlint core version listed in this table.


|Gradle Plugin|SonarLint|
|-----:|-----:|
| 1.0.0| 8.0.2.42487|


## Sonarlint rules

By default SonarLint has different rules for production code and test code. For instance for test code there is a rule that checks for asserts in unit tests.

