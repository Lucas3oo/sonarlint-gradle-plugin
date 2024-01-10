# sonarlint-gradle-plugin
![build workflow](https://github.com/Lucas3oo/sonarlint-gradle-plugin/actions/workflows/build.yaml/badge.svg)
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-se.solrike.sonarlint-blue.svg)](https://plugins.gradle.org/plugin/se.solrike.sonarlint)


Gradle plugin for sonarlint code analysis.
Supports Java, Node, Kotlin and Scala.
But possible to configure it for other languages that sonarlint has plugins for like Ruby and Golang.


1. [Usage](#usage)
    1. [Apply to your project](#apply-to-your-project)
    1. [Tasks in this plugin](#tasks-in-this-plugin)
    1. [Configure sonarlint Plugin](#configure-sonarlint-plugin)
    1. [Apply to Java project](#apply-to-java-project)
    1. [Apply to Node project](#apply-to-node-project)
    1. [Apply to Kotlin project](#apply-to-kotlin-project)
    1. [Apply to Scala project](#apply-to-scala-project)
    1. [Apply to Xxx project](#apply-to-xxx-project)
1. [sonarlint version mapping](#sonarlint-version-mapping)
1. [sonarlint rules](#sonarlint-rules)
    1. [Suppress rules in Java](#suppress-rules-in-java)
    1. [Suppress rules in most languages](#suppress-rules-in-most-languages)
1. [sonarlint CI reports](#sonarlint-ci-reports)
    1. [Github actions using Spotbugs format](#github-actions-using-spotbugs-format)
    1. [Github actions using SARIF format](#github-actions-using-sarif-format)
    1. [AWS CodeCatalyst using SARIF format](#aws-codecatalyst-using-sarif-format)
    1. [Azure DevOps using SARIF format](#azure-devops-using-sarif-format)
1. [sonarlint plugins](#sonarlint-plugins)
1. [Release notes](#release-notes)




## Usage

### Apply to your project

Apply the plugin to your project.

```groovy
plugins {
  id 'se.solrike.sonarlint' version '2.0.0'
}
```

Gradle 7.0 or later must be used.


### Tasks in this plugin
The plugin defines two tasks; one for the actual linting and one to list available rules and configuration for the rules.
In a Java project there will be one sonarlint task automatically created for each source  set.
Typically `sonarlintMain` and `sonarlintTest`, see more below.
The task for listing the rules `sonarlintListRules` has to be manually created, see more below.

```groovy
task sonarlintListRules(type: se.solrike.sonarlint.SonarlintListRules) {
  description = 'List sonarlint rules'
  group = 'verification'
}
```


### Configure sonarlint Plugin

Configure `sonarlint` extension to configure the behavior of tasks:

```groovy
sonarlint {
  excludeRules = ['java:S1186']
  includeRules = ['java:S1176', 'java:S1696', 'java:S4266']
  ignoreFailures = false
  maxIssues = 0 // default 0
  reportsDir = 'someFolder' // default build/reports/sonarlint
  // note that rule parameter names are case sensitive
  ruleParameters = [
    'java:S1176' : [
      'forClasses':'**.api.**',      // need javadoc for public methods in package matching 'api'
      'exclusion': '**.private.**'] // do not need javadoc for classes under 'private'. Default is **.internal.**
  ]
  showIssues = true // default true
}
```

Configure `sonarlintPlugins` to apply any sonarlint plugin:

```groovy
dependencies {
  sonarlintPlugins 'org.sonarsource.html:sonar-html-plugin:3.6.0.3106'
  sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.30.1.34514'
  sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:10.0.1.20755' // both JS and TS but requires com.github.node-gradle.node
  sonarlintPlugins 'org.sonarsource.kotlin:sonar-kotlin-plugin:2.13.0.2116'
  sonarlintPlugins 'org.sonarsource.php:sonar-php-plugin:3.25.0.9077'
  sonarlintPlugins 'org.sonarsource.python:sonar-python-plugin:3.17.0.10029'
  sonarlintPlugins 'org.sonarsource.slang:sonar-go-plugin:1.12.0.4259'
  sonarlintPlugins 'org.sonarsource.slang:sonar-ruby-plugin:1.11.0.3905'
  sonarlintPlugins 'org.sonarsource.slang:sonar-scala-plugin:1.11.0.3905'
  sonarlintPlugins 'org.sonarsource.sonarlint.omnisharp:sonarlint-omnisharp-plugin:1.4.0.50839'
  sonarlintPlugins 'org.sonarsource.text:sonar-text-plugin:2.0.1.611'
  sonarlintPlugins 'org.sonarsource.xml:sonar-xml-plugin:2.6.1.3686'
  // include a plugin not in Maven repo but can be grabbed from the IDEs
  sonarlintPlugins files("${System.getProperty('user.home')}/.p2/pool/plugins/org.sonarlint.eclipse.core_7.2.1.42550/plugins/sonar-secrets-plugin-1.1.0.36766.jar")
  sonarlintPlugins files("./sonar-cfamily-plugin-6.43.0.61486.jar")
}
```

### Apply to Java project

Apply this plugin with [the `java` plugin](https://docs.gradle.org/current/userguide/java_plugin.html) to your project,
then `Sonarlint` task will be generated for each existing sourceSet. E.g. `sonarlintMain` and `sonarlintTest`

If [the `java-test-fixtures` plugin](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures)
 is applied then the task will be called `sonarlintTestFixtures` since there will be a source set called testFixtures.


#### Configure the sonarlint Task

Configure `Sonarlint` directly, to set task-specific properties.

```groovy
// Example to configure HTML report
sonarlintMain {
  reports {
    text.enabled = false // default false
    html {
      enabled = true // default false
      // default location build/reports/sonarlint/sonarlintMain.html
      outputLocation = layout.buildDirectory.file('my_sonarlint_super_report.html')
    }
    xml.enabled = false // default false
    sarif.enabled = false // default false
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
```groovy
// Exclude files from the scan (e.g. generated source code):
sonarlintMain {
  exclude '**/org/example/some/package1/*'
  exclude '**/org/example/some/package2/*'
}
```


### Apply to Node project

Apply this plugin with the [com.github.node-gradle.node](https://plugins.gradle.org/plugin/com.github.node-gradle.node) plugin to your project and configure it to download node executable,
then `Sonarlint` task will be generated for main and test classes E.g. `sonarlintNodeMain` and `sonarlintNodeTest`

Unlike with the Java plugin the source sets needs to be assigned manually.

Sonarlint needs a node executable in order to perform the analysis. This plugin will get the location of node executable from the Node plugin and the Node plugin needs to be configured to download node.

#### Configure the sonarlint Task when using Node plugin

This example has TypeScript code under `projects/` and `src/`

```groovy
plugins {
  id 'base'
  id 'com.github.node-gradle.node' version '5.0.0'
  id 'se.solrike.sonarlint' version '2.0.0'
}
repositories {
  mavenCentral()
}
node {
  // Version of node to use.
  version = '18.9.1'
  // If empty, the plugin will use the npm command bundled with Node.js
  npmVersion = ''
  // If true, it will download node using above parameters.
  download = true
}
sonarlintNodeMain {
  ignoreFailures = false
  source = fileTree('src')
  exclude '**/*.spec.ts'
}
sonarlintNodeTest {
  ignoreFailures = true
  source = fileTree('src')
  include '**/*.spec.ts'
  isTestSource = true
}

dependencies {
  sonarlintPlugins 'org.sonarsource.html:sonar-html-plugin:3.6.0.3106'
  sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:10.0.1.20755' // both JS and TS
  sonarlintPlugins 'org.sonarsource.xml:sonar-xml-plugin:2.6.1.3686'
}
```

### Apply to Kotlin project
If the project has [the `kotlin` plugin](https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm) applied then that
means the Java plugin is applied too.
The `Sonarlint` task will be generated for main and test classes. E.g. `sonarlintMain` and `sonarlintTest`.
The source code and resources for the 'main' and 'test' source sets will be scanned using all the sonarlint plugins you
configure. So if you have both Java and Kotlin source code then configure all
the language plugins for your code. Like both `org.sonarsource.java:sonar-java-plugin:7.30.1.34514`
and `org.sonarsource.kotlin:sonar-kotlin-plugin:2.20.0.4382` and any additionally plugin you need.

Typical `gradle.build.kts`:

```kotlin
plugins {
  // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
  id("org.jetbrains.kotlin.jvm") version "1.7.21"
  id("se.solrike.sonarlint") version "2.0.0"
}

repositories {
  mavenCentral()
}

dependencies {
  // Align versions of all Kotlin components
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
  // Use the Kotlin JDK 8 standard library.
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  // This dependency is used internally, and not exposed to consumers on their own compile classpath.
  implementation("com.google.guava:guava:31.0.1-jre")
  // Use the Kotlin test library.
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  // Use the Kotlin JUnit integration.
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

  sonarlintPlugins("org.sonarsource.kotlin:sonar-kotlin-plugin:2.20.0.4382")
}

// configure the tasks directly since the overloaded "extensions" like sonarlintMain will not work in Kotlin DSL
tasks.sonarlintMain {
  maxIssues.set(1)
}

```

### Apply to Scala project
If the project has [the `scala` plugin](https://docs.gradle.org/current/userguide/scala_plugin.html) applied then that
means the Java plugin is applied too.
The `Sonarlint` task will be generated for main and test classes. E.g. `sonarlintMain` and `sonarlintTest`.
The source code and resources for the 'main' and 'test' source sets will be scanned using all the sonarlint plugins you
configure. So if you have both Java and Scala source code then configure all
the language plugins for your code. Like both `org.sonarsource.java:sonar-java-plugin:7.17.0.31219`
and `org.sonarsource.slang:sonar-scala-plugin:1.11.0.3905` and any additionally plugin you need.


```groovy
plugins {
  id 'scala'
  id 'se.solrike.sonarlint' version '2.0.0'
}

repositories {
  mavenCentral()
}

dependencies {
  // Use Scala 2.13 in our library project
  implementation 'org.scala-lang:scala-library:2.13.7'
  // This dependency is used internally, and not exposed to consumers on their own compile classpath.
  implementation 'com.google.guava:guava:31.0.1-jre'
  // Use Scalatest for testing our library
  testImplementation 'junit:junit:4.13.2'
  testImplementation 'org.scalatest:scalatest_2.13:3.2.10'
  testImplementation 'org.scalatestplus:junit-4-13_2.13:3.2.2.0'
  // Need scala-xml at test runtime
  testRuntimeOnly 'org.scala-lang.modules:scala-xml_2.13:1.2.0'

  sonarlintPlugins 'org.sonarsource.slang:sonar-scala-plugin:1.11.0.3905'
}

sonarlintMain {
  excludeRules = ['scala:S1481']
  ignoreFailures = true
}
```


### Apply to Xxx project
Only a subset of languages are supported by Sonarlint. For other languages the plugin will not be auto applied.
Instead it has to be defined explicitly in the `build.gradle`.

#### Create the sonarlint task when it is not auto created

```groovy
plugins {
  id 'base'
  id 'se.solrike.sonarlint' version '2.0.0'
}
repositories {
  mavenCentral()
}
task sonarlintMain(type: se.solrike.sonarlint.Sonarlint) {
  description = 'Runs sonarlint on main code'
  group = 'verification'
  ignoreFailures = false
  source = fileTree('my_src_folder')
}
check.dependsOn = ['sonarlintMain']

dependencies {
  // configure the sonarlint secrets plugin for finding secret (like hardcoded access keys for AWS) in the code.
  sonarlintPlugins files("${System.getProperty('user.home')}/.p2/pool/plugins/org.sonarlint.eclipse.core_7.2.1.42550/plugins/sonar-secrets-plugin-1.1.0.36766.jar")
}
```


## sonarlint version mapping

By default, this Gradle Plugin uses the [sonarlint core](https://github.com/SonarSource/sonarlint-core) version listed in this table.



|Gradle Plugin|sonarlint|
|-----:|-----:|
| 1.0.0| 8.0.2.42487|
| 2.0.0| 9.6.1.76766|


## sonarlint rules

By default sonarlint has different rules for production code and test code. For instance for test code there is a rule that checks for asserts in unit tests.

Rules are described [here](https://rules.sonarsource.com/). Note that some rules are for SonarQube or SonarCloud only.


To list all the rules in your configured plugins you will have to create the task manually. Complete example:

```groovy
plugins {
  id 'se.solrike.sonarlint' version '2.0.0'
  id 'com.github.node-gradle.node' version '5.0.0'
}
repositories {
  mavenCentral()
}
dependencies {
  sonarlintPlugins 'org.sonarsource.html:sonar-html-plugin:3.6.0.3106'
  sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.17.0.31219'
  sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:10.0.1.20755' // both JS and TS
  sonarlintPlugins 'org.sonarsource.kotlin:sonar-kotlin-plugin:2.20.0.4382'
  sonarlintPlugins 'org.sonarsource.php:sonar-php-plugin:3.25.0.9077'
  sonarlintPlugins 'org.sonarsource.python:sonar-python-plugin:3.17.0.10029'
  sonarlintPlugins 'org.sonarsource.slang:sonar-ruby-plugin:1.11.0.3905'
  sonarlintPlugins 'org.sonarsource.slang:sonar-scala-plugin:1.11.0.3905'
  sonarlintPlugins 'org.sonarsource.sonarlint.omnisharp:sonarlint-omnisharp-plugin:1.4.0.50839'
  sonarlintPlugins 'org.sonarsource.xml:sonar-xml-plugin:2.6.1.3686'
  sonarlintPlugins files("./sonar-secrets-plugin-1.1.0.36766.jar")
}
node {
  version = '18.9.1'
  // If empty, the plugin will use the npm command bundled with Node.js
  npmVersion = ''
  download = true
}

task sonarlintListRules(type: se.solrike.sonarlint.SonarlintListRules) {
  description = 'List sonarlint rules'
  group = 'verification'
}

sonarlint {
  excludeRules = ['java:S1185']
  includeRules = ['java:S1176', 'Web:LongJavaScriptCheck']
  ruleParameters = [
    'java:S1176' : [
      'forClasses':'**.api.**',
      'exclusion': '**.private.**']  // default **.internal.**
  ]
}
```

When the task `sonarlintListRules` is executed the list on the console will be similar to (shorted):

```
...
[x] csharpsquid:S4423 - Weak SSL/TLS protocols should not be used - [cwe, owasp-a3, owasp-a6, owasp-m3, privacy, sans-top25-porous] - C#
[x] csharpsquid:S4426 - Cryptographic keys should be robust - [cwe, owasp-a3, owasp-a6, owasp-m5, privacy] - C#
...
[x] java:S1175 - The signature of "finalize()" should match that of "Object.finalize()" - [pitfall] - Java
[x] java:S1176 - Public types, methods and fields (API) should be documented with Javadoc - [convention] - Java
    forClasses : **.api.**
    exclusion : **.private.**
[x] java:S1181 - Throwable and Error should not be caught - [bad-practice, cert, cwe, error-handling] - Java
[x] java:S1182 - Classes that override "clone" should be "Cloneable" and call "super.clone()" - [cert, convention, cwe] - Java
[ ] java:S1185 - Overriding methods should do more than simply call the same method in the super class - [clumsy, redundant] - Java
[x] java:S1186 - Methods should not be empty - [suspicious] - Java
[ ] java:S1188 - Anonymous classes should not have too many lines - [] - Java
    Max : 20
[x] java:S1190 - Future keywords should not be used as names - [obsolete, pitfall] - Java
...
[ ] xml:S1120 - Source code should be indented consistently - [convention] - XML
    tabSize : 2
    indentSize : 2
[x] xml:S1134 - Track uses of "FIXME" tags - [cwe] - XML
[x] xml:S1135 - Track uses of "TODO" tags - [cwe] - XML
...
```

The first column indicates if the rule is enabled/included or not.

Since most of the rules have tags like 'aws', 'tests', 'regex' so you can easily grep on those. E.g.:

    ./gradlew sonarlintListRules | grep aws

And the result will be:

```
[x] java:S6241 - Region should be set explicitly when creating a new "AwsClient" - [aws, startup-time] - Java
[x] java:S6242 - Credentials Provider should be set explicitly when creating a new "AwsClient" - [aws, startup-time] - Java
[x] java:S6243 - Reusable resources should be initialized at construction time of Lambda functions - [aws] - Java
[x] java:S6244 - Consumer Builders should be used - [aws] - Java
[x] java:S6246 - Lambdas should not invoke other lambdas synchronously - [aws] - Java
[x] java:S6262 - AWS region should not be set with a hardcoded String - [aws] - Java
```

### Suppress rules in Java
If you need to deactivate a rule for a project then add the rule to the `excludeRules` list.
If you need to just suppress an issue in a file you can use `@SuppressWarnings("all")` or `@SuppressWarnings` with rule keys: `@SuppressWarnings("java:S2077")` or `@SuppressWarnings({"java:S1118", "java:S3546"})`.
(In Eclipse you might need to suppress the warning about unhandled token in the annotation.)


### Suppress rules in most languages
In for instance TypeScript you can disable a rule in a specifc file by add `// NOSONAR` or `//NOSONAR` at the end of the line with the issue. E.g.

```typescript
const key = 'AKIATCHLSJSHD' // NOSONAR AWS access key must be used here.
```

## sonarlint CI reports
The sonarlint gradle plugin can generate Spotbugs/Findbugs compatible XML files and also SARIF compatible JSON files.

Enable as follows:

```groovy
sonarlintMain {
  reports {
    xml.enabled = true // default false
    sarif.enabled = true // default false
  }
}
```


### Github actions using Spotbugs format
If you are using Github actions you can use the same action for sonarlint as you are using for Spotbugs.
(Remember to customize name and title for the second definition of the spotbugs action.)

```yaml
name: build
run-name: ${{ github.actor }} is building the gradle project
on: [push]
jobs:
  build-main-artifact:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version-file: ./.java-version
          distribution: temurin
          cache: gradle
      - run: ./gradlew build --no-daemon
      - name: Publish Test Results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: |
            build/test-results/test/*.xml
      - name: Publish Spotbugs Results
        uses: jwgmeligmeyling/spotbugs-github-action@v1.2
        with:
          name: Spotbugs
          path: build/reports/spotbugs/*.xml
      - name: Publish Sonarlint Results
        uses: jwgmeligmeyling/spotbugs-github-action@v1.2
        with:
          name: Sonarlint
          title: Sonarlint report
          path: build/reports/sonarlint/*.xml
      - uses: actions/upload-artifact@v3
        with:
          name: Package
          path: build/libs
```

### Github actions using SARIF format
If you are using Github actions you can use the generic SARIF plugin to let Github display the issues in the "Security" tab.

```yaml
name: build
run-name: ${{ github.actor }} is building the gradle project
on: [push]
jobs:
  build-main-artifact:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version-file: ./.java-version
          distribution: temurin
          cache: gradle
      - run: ./gradlew build --no-daemon
      - name: Publish Test Results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: |
            build/test-results/test/*.xml
      - name: Sonarlint
        uses: github/codeql-action/upload-sarif@v2
        # The issues will be visible in the security tab in github
        with:
          # Path to SARIF file relative to the root of the repository or path to a folder with sarif files
          # wildcard doesnt work!
          sarif_file: build/reports/sonarlint/sonarlintMain.sarif
          # Optional category for the results
          # Used to differentiate multiple results for one commit
          category: Sonarlint
      - name: Spotbugs
        uses: github/codeql-action/upload-sarif@v2
        # The issues will be visible in the security tab in github
        with:
          sarif_file: build/reports/spotbugs/main.sarif
          category: Spotbugs
      - name: Archive main artifacts
        uses: actions/upload-artifact@v3
        with:
          name: Artifacts
          path: build/libs
```

### AWS CodeCatalyst using SARIF format
If you are using AWS CodeCatalyst you can turn on "auto discover" for reports and the Sonarlint report will be under the "Reports" tab.

```yaml
Name: Workflow_7f90
SchemaVersion: "1.0"
Triggers:
  - Type: PUSH
    Branches:
      - main
Actions:
  Build_50:
    Identifier: aws/build@v1.0.0
    Outputs:
      AutoDiscoverReports:
        Enabled: true
        ReportNamePrefix: rpt
    Compute:
      Type: EC2
      Fleet: Linux.x86-64.Large
    Inputs:
      Sources:
        - WorkflowSource
    Configuration:
      Steps:
        - Run: ./gradlew build --no-daemon
```

### Azure DevOps using SARIF format
You must install "SARIF SAST Scans Tab" from the marketplace into the Azure DevOps organization. Then there will be a "Scans" tab next to the "Tests" tab.

```yaml
... snippet
# The SARIF files need to go to a artifact called "CodeAnalysisLogs"
- task: PublishBuildArtifacts@1
  displayName: 'Sonarlint report'
  inputs:
    # Wildcards are not supported!!!
    pathToPublish: build/reports/sonarlint/sonarlintMain.sarif
    artifactName: CodeAnalysisLogs
  condition: succeededOrFailed()
```



## sonarlint plugins
* [Java](https://github.com/SonarSource/sonar-java/blob/master/sonar-java-plugin/src/main/resources/static/documentation.md)
* [JavaScript/TypeScript/CSS](https://github.com/SonarSource/SonarJS/blob/master/sonar-javascript-plugin/src/main/resources/static/documentation.md)
* [Kotlin](https://github.com/SonarSource/sonar-kotlin/blob/master/sonar-kotlin-plugin/src/main/resources/static/documentation.md)


## Release notes
### 2.0.0
Support for Sonarlint core 9.6 which means that newer plugins can be used.
Like org.sonarsource.java:sonar-java-plugin:7.30.1.34514.

### 1.0.0-beta.17
If any problems with the sonarlint plugins are found, the build will break. For instance using a too new plugin version or
missing dependencies or runtime (like NodeJS). This address issue [issue 5](https://github.com/Lucas3oo/sonarlint-gradle-plugin/issues/5)
where Sonarlint silently continues even if some plugins fail to load.

### 1.0.0-beta.16
Support for [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html).
Contributed by [Lukas Gräf](https://github.com/lukasgraef).

### 1.0.0-beta.15
Fix bug in detecting CPU architecture for selecting correct node binaries.

Improve the documentation.

### 1.0.0-beta.14
Adding support for reports in Static Analysis Results Interchange Format (SARIF) format by OASIS. See https://sarifweb.azurewebsites.net.
This means that a standard format is used for reporting static code analysis findings. Github actions, Azure DevOps and AWS CodeCatalyst are supporting this format.

Fix formating of the description. Sonarlint only offers HTML based description and it renders nice the Github action but not so nice in the Security tab.

### 1.0.0-beta.9
Adding option to generate Spotbugs/Findbugs XML for the issues so for instance Jenkins' code quality reports can be used.
Also Github action from https://github.com/jwgmeligmeyling/spotbugs-github-action can be used.
Include reference to the Golang sonarlint plugin.

### 1.0.0-beta.8
Adding task to list all the rules and how they are configured.

Re-think about the Kotlin support. In fact the sonarlintMain task created will cover Kotlin code too since
the Kotlin plugin is also applying the Java plugin. So there is no need for dedicated Sonarlint task for Kotlin.

### 1.0.0-beta.7
Add support for Kotlin projects. The tasks for Kotlin will be automatically created.

### 1.0.0-beta.6
The working folder for the Sonarlint engine is now properly under the project's build folder. Sonarlint's user home is the project's folder.

### 1.0.0-beta.5
Fix typo in printouts. Thanks @doofy for contributing!

### 1.0.0-beta.4
Fix OS and architecture detection for node executable when running on amd64.

### 1.0.0-beta.3
* when using test fixture plugin the source set wasn't marked as test source so wrong set of sonarlint rules was applied.
* include summary and TOC in the html report

### 1.0.0-beta.2
* Fix html report issue with rule Web:S5256. Some rules description has html tags which are not escaped.
* When a report is generate the path will be printed on the console.

### 1.0.0-beta.1
Sonarlint analysis for Java and Node(JavaScript/TypeScript).
For Node projects the node plugin `com.github.node-gradle.node` needs to be configured to download node.
This plugin picks the node executable from that but since the node path contains info about the OS and architecture it is a bit messy to get that correct on all platforms. Right now it is only tested on mac and linux on x86 platform.


### future
Improvements that might be implemented are:
* Support for windows when using node.
* Support to specify sonarlint properties. For instance the node exec can be configured that way using 'sonar.nodejs.executable'.
* Support to find node on the $PATH using org.sonarsource.sonarlint.core.NodeJsHelper
* Support for a list of suppressed issues like Checkstyle and Spotbug have.
* specify stylesheet for the html reports
* Be able to specify the source sets in the sonarlint DSL
* specify the sonarlint-core version via a toolsversion property and invoke it via WorkerAPI
* make sure up-to-date checks are resonable
* link to rules description in the report
* it might exists more issue types: "SECURITY_HOTSPOT" but they are not in sonarlint


SARIF
 - partialFingerprints on the result
 A set of strings used to track the unique identity of the result. Code scanning uses partialFingerprints to accurately identify which results are the same across commits and branches.
 Note: Code scanning only uses the primaryLocationLineHash.

           "partialFingerprints": {
            "primaryLocationLineHash": "39fa2ee980eb94b0:1"
          }

