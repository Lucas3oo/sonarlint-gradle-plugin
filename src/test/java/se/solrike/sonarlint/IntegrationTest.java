package se.solrike.sonarlint;

import static org.assertj.core.api.Assertions.*;

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
      writer.write("repositories {\n"
          + "  mavenCentral()\n"
          + "}\n");
      writer.write("sonarlint {\n"
          + "  dependencies {\n"
          + "    sonarlintPlugins 'org.sonarsource.html:sonar-html-plugin:3.6.0.3106'\n"
          + "    sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.8.1.28740'\n"
          + "    sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:8.8.0.17228' // both JS and TS\n"
          + "    sonarlintPlugins 'org.sonarsource.typescript:sonar-typescript-plugin:2.1.0.4359'\n"
          + "    sonarlintPlugins 'org.sonarsource.xml:sonar-xml-plugin:2.5.0.3376'\n"
          + "  }\n"
          + "}\n");
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
    BuildResult buildResult = runGradle(false, List.of("sonarlintMain"));

    // then the gradle build shall fail
    assertThat(buildResult.task(":sonarlintMain").getOutcome()).isEqualTo(TaskOutcome.FAILED);
    // and the 3 sonarlint rules violated are
    assertThat(buildResult.getOutput()).contains("3 SonarLint issue(s) were found.");
    assertThat(buildResult.getOutput()).contains("java:S1186", "java:S1118", "java:S1220");

    System.err.println(buildResult.getOutput());

  }

  BuildResult runGradle(List<String> args) {
    return runGradle(true, args);
  }

  BuildResult runGradle(boolean isSuccessExpected, List<String> args) {
    GradleRunner gradleRunner = GradleRunner.create()
        .withDebug(true)
        .withArguments(args)
        .withProjectDir(mProjectDir.toFile())
        .withPluginClasspath(); // to get plugin under test found by the runner
    return isSuccessExpected ? gradleRunner.build() : gradleRunner.buildAndFail();
  }

  void createJavaFile(Path javaFile) {
    // @formatter:off
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(javaFile.toFile()))) {
      writer.write("public class Hello {\n"
          + "\n"
          + "  public static void get() {\n"
          + "\n"
          + "  }\n"
          + "}\n");
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    // @formatter:on

  }

}
