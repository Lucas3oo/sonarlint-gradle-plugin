# sonarlint-gradle-plugin
![build workflow](https://github.com/Lucas3oo/sonarlint-gradle-plugin/actions/workflows/build.yaml/badge.svg)


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
1. [sonarlint CI reports](#sonarlint-ci-reports)
    1. [Github actions](#github-actions)
1. [sonarlint plugins](#sonarlint-plugins)
1. [Release notes](#release-notes)




## Usage

### Apply to your project

Apply the plugin to your project.

```groovy
plugins {
  id 'se.solrike.sonarlint' version '1.0.0-beta.9'
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

Configure `sonarlint` extension to configure the behaviour of tasks:

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
  sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.17.0.31219'
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

#### Configure the sonarlint Task when using Node plugin

This example has TypeScript code under `projects/` and `src/`

```groovy
plugins {
  id 'base'
  id 'com.github.node-gradle.node' version '3.2.1'
  id 'se.solrike.sonarlint' version '1.0.0-beta.9'
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
the language plugins for your code. Like both `org.sonarsource.java:sonar-java-plugin:7.17.0.31219`
and `org.sonarsource.kotlin:sonar-kotlin-plugin:2.13.0.2116` and any additionally plugin you need.

Typical `gradle.build.kts`:

```kotlin
plugins {
  // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
  id("org.jetbrains.kotlin.jvm") version "1.7.21"
  id("se.solrike.sonarlint") version "1.0.0-beta.9"
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

  sonarlintPlugins("org.sonarsource.kotlin:sonar-kotlin-plugin:2.13.0.2116")
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
  id 'se.solrike.sonarlint' version '1.0.0-beta.9'
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
  id 'se.solrike.sonarlint' version '1.0.0-beta.9'
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


## sonarlint rules

By default sonarlint has different rules for production code and test code. For instance for test code there is a rule that checks for asserts in unit tests.

Rules are described [here](https://rules.sonarsource.com/). Note that some rules are for SonarQube or SonarCloud only.


To list all the rules in your configured plugins you will have to create the task manually. Complete example:

```groovy
plugins {
  id 'se.solrike.sonarlint' version '1.0.0-beta.9'
  id 'com.github.node-gradle.node' version '3.2.1'
}
repositories {
  mavenCentral()
}
dependencies {
  sonarlintPlugins 'org.sonarsource.html:sonar-html-plugin:3.6.0.3106'
  sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.17.0.31219'
  sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:10.0.1.20755' // both JS and TS
  sonarlintPlugins 'org.sonarsource.kotlin:sonar-kotlin-plugin:2.13.0.2116'
  sonarlintPlugins 'org.sonarsource.php:sonar-php-plugin:3.25.0.9077'
  sonarlintPlugins 'org.sonarsource.python:sonar-python-plugin:3.17.0.10029'
  sonarlintPlugins 'org.sonarsource.slang:sonar-ruby-plugin:1.11.0.3905'
  sonarlintPlugins 'org.sonarsource.slang:sonar-scala-plugin:1.11.0.3905'
  sonarlintPlugins 'org.sonarsource.sonarlint.omnisharp:sonarlint-omnisharp-plugin:1.4.0.50839'
  sonarlintPlugins 'org.sonarsource.xml:sonar-xml-plugin:2.6.1.3686'
  sonarlintPlugins files("./sonar-secrets-plugin-1.1.0.36766.jar")
}
node {
  version = '14.17.2'
  npmVersion = '6.14.13'
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

Since most of the rules have tags you can easily grep on those. E.g.:

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


## sonarlint CI reports
The sonarlint gradle plugin can generate Spotbugs/Findbugs compatible XML reports

Enable it as follows:

```groovy
sonarlintMain {
  reports {
    xml.enabled = false // default false
  }
}
```

### Github actions
If you are using Github actions you can use the same action for sonarlint as you are using for Spotbugs. E.g:


```yaml
```




## sonarlint plugins
* [Java](https://github.com/SonarSource/sonar-java/blob/master/sonar-java-plugin/src/main/resources/static/documentation.md)
* [JavaScript/TypeScript/CSS](https://github.com/SonarSource/SonarJS/blob/master/sonar-javascript-plugin/src/main/resources/static/documentation.md)
* [Kotlin](https://github.com/SonarSource/sonar-kotlin/blob/master/sonar-kotlin-plugin/src/main/resources/static/documentation.md)


## Release notes
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
* Support to specify sonarlint properties. For instance the node exec can be configured that way using 'sonar.nodejs.executable'.
* Support to find node on the $PATH using org.sonarsource.sonarlint.core.NodeJsHelper
* Support for a list of suppressed issues like Checkstyle and Spotbug have.
* specify stylesheet for the html reports
* Be able to specify the source sets in the sonarlint DSL
* specify the sonarlint-core version via a toolsversion property and invoke it via WorkerAPI
* make sure up-to-date checks are resonable
* link to rules description in the report
* it might exists more issue types: "SECURITY_HOTSPOT" but they are not in sonarlint

