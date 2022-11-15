# sonarlint-gradle-plugin
Gradle plugin for SonarLint code analysis.
Supports Java, Node, Kotlin and Scala.
But possible to configure it for other languages that Sonarlint has plugins for like Ruby.


[[_TOC_]]

## Usage

### Apply to your project

Apply the plugin to your project.

```groovy
plugins {
  id 'se.solrike.sonarlint' version '1.0.0-beta.8'
}
```

Gradle 7.0 or later must be used.

### Configure SonarLint Plugin

Configure `sonarlint` extension to configure the behaviour of tasks:

```groovy
sonarlint {
  excludeRules = ['java:S1186']
  includeRules = ['java:S1696', 'java:S4266']
  ignoreFailures = false
  maxIssues = 0 // default 0
  reportsDir = 'someFolder' // default build/reports/sonarlint
  // note that rule parameter names are case sensitive
  ruleParameters = [
    'java:S1176' : [
      'forClasses':'**.api.**',      // need javadoc for public methods in package matching 'api'
      'exclusion': '**.internal.**'] // do not need javadoc for classes under 'internal'
  ]
  showIssues = true // default true
}
```

Configure `sonarlintPlugins` to apply any SonarLint plugin:

```groovy
dependencies {
  sonarlintPlugins 'org.sonarsource.html:sonar-html-plugin:3.6.0.3106'
  sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.15.0.30507'
  sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:9.9.0.19492' // both JS and TS
  sonarlintPlugins 'org.sonarsource.typescript:sonar-typescript-plugin:2.1.0.4359'
  sonarlintPlugins 'org.sonarsource.xml:sonar-xml-plugin:2.6.1.3686'
  // include a plugin not in Maven repo
  sonarlintPlugins files("${System.getProperty('user.home')}/.p2/pool/plugins/org.sonarlint.eclipse.core_7.2.1.42550/plugins/sonar-secrets-plugin-1.1.0.36766.jar")
}
```


### Apply to Java project

Apply this plugin with [the `java` plugin](https://docs.gradle.org/current/userguide/java_plugin.html) to your project,
then `Sonarlint` task will be generated for each existing sourceSet. E.g. `sonarlintMain` and `sonarlintTest`

If [the `java-test-fixtures` plugin](https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures)
 is applied then the task will be called `sonarlintTestFixtures` since there will be a source set called testFixtures.


#### Configure the SonarLint Task

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


### Apply to Node project

Apply this plugin with the [com.github.node-gradle.node](https://plugins.gradle.org/plugin/com.github.node-gradle.node) plugin to your project and configure it to download node executable,
then `Sonarlint` task will be generated for main and test classes E.g. `sonarlintNodeMain` and `sonarlintNodeTest`

Unlike with the Java plugin the source sets needs to be assigned manually.

Sonarlint needs a node executable in order to perform the analysis. This plugin will get the location of node executable from the Node plugin and the Node plugin needs to be configured to download node.

#### Configure the SonarLint Task when using Node plugin

This example has TypeScript code under `projects/` and `src/`

```groovy
plugins {
  id 'base'
  id 'com.github.node-gradle.node' version '3.2.1'
  id 'se.solrike.sonarlint' version '1.0.0-beta.8'
}
repositories {
  mavenCentral()
}
node {
  // Version of node to use.
  version = '14.17.2'
  // Version of npm to use.
  npmVersion = '6.14.13'
  // If true, it will download node using above parameters.
  download = true
}
sonarlintNodeMain {
  maxIssues = 2
  ignoreFailures = false
}
sonarlintNodeTest {
  ignoreFailures = true
}
tasks.named('sonarlintNodeMain') {
  def projectSrc = fileTree('projects').exclude('**/*.spec.ts')
  def appSrc = fileTree('src').exclude('**/*.spec.ts')
  source = projectSrc.plus(appSrc)
}
tasks.named('sonarlintNodeTest') {
  def projectSrc = fileTree('projects').include('**/*.spec.ts')
  def appSrc = fileTree('src').include('**/*.spec.ts')
  source = projectSrc.plus(appSrc)
  isTestSource = true
}
dependencies {
  sonarlintPlugins 'org.sonarsource.html:sonar-html-plugin:3.6.0.3106'
  sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:9.9.0.19492' // both JS and TS
  sonarlintPlugins 'org.sonarsource.typescript:sonar-typescript-plugin:2.1.0.4359'
  sonarlintPlugins 'org.sonarsource.xml:sonar-xml-plugin:2.6.1.3686'
}
```

### Apply to Kotlin project
If the project has the Kotlin plugin applied then that means the Java plugin is applied too.
The `Sonarlint` task will be generated for main and test classes. E.g. `sonarlintMain` and `sonarlintTest`.
The source code and resources for the 'main' source set will be scanned using all the SonarLint plugins you
configure. So if you have both Java and Kotlin source code then configure all
the language plugins for your code. Like both `org.sonarsource.java:sonar-java-plugin:7.15.0.30507`
and `org.sonarsource.kotlin:sonar-kotlin-plugin:2.10.0.1456` and any additionally plugin you need.

Typical `gradle.build.kts`:

```kotlin
plugins {
  // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
  id("org.jetbrains.kotlin.jvm") version "1.6.21"
  id("se.solrike.sonarlint") version "1.0.0-beta.8"
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

  sonarlintPlugins("org.sonarsource.kotlin:sonar-kotlin-plugin:2.10.0.1456")
}

// configure the tasks directly since the overloaded "extensions" like sonarlintMain will really work in Kotlin DSL
tasks.sonarlintMain {
  maxIssues.set(1)
}

```

### Apply to Scala project
If the project has the Scala plugin applied then that means the Java plugin is applied too.
The `Sonarlint` task will be generated for main and test classes. E.g. `sonarlintMain` and `sonarlintTest`.
The source code and resources for the 'main' source set will be scanned using all the SonarLint plugins you
configure. So if you have both Java and Scala source code then configure all
the language plugins for your code. Like both `org.sonarsource.java:sonar-java-plugin:7.15.0.30507`
and `org.sonarsource.slang:sonar-scala-plugin:1.11.0.3905` and any additionally plugin you need.



```groovy
plugins {
  id 'scala'
  id 'se.solrike.sonarlint' version '1.0.0-beta.8'
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

#### Create the SonarLint Task when it is not auto created

```groovy
plugins {
  id 'base'
  id 'se.solrike.sonarlint' version '1.0.0-beta.8'
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




## Sonarlint version mapping

By default, this Gradle Plugin uses the [Sonarlint core](https://github.com/SonarSource/sonarlint-core) version listed in this table.



|Gradle Plugin|SonarLint|
|-----:|-----:|
| 1.0.0| 8.0.2.42487|


## Sonarlint rules

By default SonarLint has different rules for production code and test code. For instance for test code there is a rule that checks for asserts in unit tests.

Rules are described [here](https://rules.sonarsource.com/). Note that some rules are for SonarCube or SonarCloud only.




### Suppress rules in Java
If you need to deactivate a rule for a project then add the rule to the `excludeRules` list.
If you need to just suppress an issue in a file you can use `@SuppressWarnings("all")` or `@SuppressWarnings` with rule keys: `@SuppressWarnings("java:S2077")` or `@SuppressWarnings({"java:S1118", "java:S3546"})`.
(In Eclipse you might need to suppress the warning about unhandled token in the annotation.)

## SonarLint plugins
* [Java](https://github.com/SonarSource/sonar-java/blob/master/sonar-java-plugin/src/main/resources/static/documentation.md)
* [JavaScript/TypeScript/CSS](https://github.com/SonarSource/SonarJS/blob/master/sonar-javascript-plugin/src/main/resources/static/documentation.md)


## Release notes
### 1.0.0-beta.8
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
* Support to specify sonarlint properties. For instance the node exec can be configured that way using 'sonar.nodejs.executable'.
* Support to find node on the $PATH using org.sonarsource.sonarlint.core.NodeJsHelper
* Support for a list of suppressed issues like Checkstyle and Spotbug have.
* specify stylesheet for the html reports
* Be able to specify the source sets in the sonarlint DSL
* specify the sonarlint-core version via a toolsversion property and invoke it via WorkerAPI
* generate findbugs XML so e.g. Jenkins plugins can be used to show the issues
* make sure up-to-date checks are resonable
* link to rules description in the report
* it might exists more issue types: "SECURITY_HOTSPOT" but they are not in sonarlint

