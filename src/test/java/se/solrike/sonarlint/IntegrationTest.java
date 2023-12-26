package se.solrike.sonarlint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IntegrationTest {

  @TempDir
  Path mProjectDir;
  Path mBuildFile;

  @BeforeEach
  void setup() throws IOException {
    mBuildFile = Files.createFile(mProjectDir.resolve("build.gradle"));
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(mBuildFile.toFile()))) {
      writer.write("plugins { id ('java-library') \n  id('se.solrike.sonarlint')  }\n");
      // @formatter:off
      writer.write(""
          + "repositories {\n"
          + "  mavenCentral()\n"
          + "}\n");
      writer.write(""
          + "sonarlint {\n"
          + "  dependencies {\n"
          + "    sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.17.0.31219'\n"
          + "    sonarlintPlugins 'org.sonarsource.slang:sonar-go-plugin:1.12.0.4259'\n"
          + "    sonarlintPlugins 'org.sonarsource.text:sonar-text-plugin:2.0.1.611'\n"
          + "  }\n"
          + "  includeRules = ['java:S1176']\n"
          + "}\n"
          + "task sonarlintListRules(type: se.solrike.sonarlint.SonarlintListRules) {\n"
          + "}\n"
          + "sonarlintMain {\n"
          + "  reports {"
          + "    xml.enabled = true\n"
          + "    sarif.enabled = true\n"
          + "  }\n"
          + "}\n"
          );
      // @formatter:on
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    // setup for java project
    Files.createDirectories(mProjectDir.resolve("src/main/java/"));

  }

  @Test
  void testSonarlintMain() throws IOException {
    // given a java class with
    createJavaFile(Files.createFile(mProjectDir.resolve("src/main/java/Hello.java")));

    // when sonarlintMain is run
    BuildResult buildResult = runGradle(false, List.of("--debug", "sonarlintMain"));

    // then the gradle build shall fail
    assertThat(buildResult.task(":sonarlintMain").getOutcome()).isEqualTo(TaskOutcome.FAILED);
    // and the 3 sonarlint rules violated are
    assertThat(buildResult.getOutput()).contains("3 SonarLint issue(s) were found.");
    assertThat(buildResult.getOutput()).contains("java:S1186", "java:S1118", "java:S1220");
    // since xml report is enabled the plugin shall print the location of the report
    assertThat(buildResult.getOutput()).contains("Report generated at:");

    assertThat(mProjectDir.resolve("build/reports/sonarlint/sonarlintMain.xml").toFile()).exists();
    assertThat(mProjectDir.resolve("build/reports/sonarlint/sonarlintMain.sarif").toFile()).exists();

    // CHECKSTYLE:OFF
    System.err.println(mProjectDir.resolve("build/reports/sonarlint/sonarlintMain.sarif").toFile().getAbsolutePath());
    System.err.println(buildResult.getOutput());
    // CHECKSTYLE:ON
  }

  @Test
  void testSonarlintListRules() throws IOException {
    // given java plugin is configured and the sonarlint list rules task is created in the setup
    // and java:S1176 rule is included which is default excluded.

    // when sonarlintListRules is run
    BuildResult buildResult = runGradle(true, List.of("sonarlintListRules"));

    // then the gradle build shall succeed
    assertThat(buildResult.task(":sonarlintListRules").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    // and the rules listed shall at least be
    assertThat(buildResult.getOutput()).contains("[x] java:S1176", "[ ] java:S6411", "[x] java:S6437");

    // CHECKSTYLE:OFF
    System.err.println(buildResult.getOutput());
    // CHECKSTYLE:ON
  }

  BuildResult runGradle(List<String> args) {
    return runGradle(true, args);
  }

  BuildResult runGradle(boolean isSuccessExpected, List<String> args) {
    // to get plugin under test found by the runner
    GradleRunner gradleRunner = GradleRunner.create()
        .withDebug(true)
        .withArguments(args)
        .withProjectDir(mProjectDir.toFile())
        .withPluginClasspath();
    return isSuccessExpected ? gradleRunner.build() : gradleRunner.buildAndFail();
  }

  void createJavaFile(Path javaFile) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(javaFile.toFile()))) {
      // @formatter:off
      writer.write(""
          + "public class Hello {\n"
          + "  public static void get() {\n"
          + "  }\n"
          + "}\n");
      // @formatter:on
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

}
