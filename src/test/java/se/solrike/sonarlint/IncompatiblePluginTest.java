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

class IncompatiblePluginTest {

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
          + "    sonarlintPlugins 'org.sonarsource.java:sonar-java-plugin:7.20.0.31692'\n"
          + "    sonarlintPlugins 'org.sonarsource.text:sonar-text-plugin:2.0.1.611'\n"
          + "    //sonarlintPlugins 'org.sonarsource.javascript:sonar-javascript-plugin:10.0.1.20755'\n"
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
    BuildResult buildResult = runGradle(false, List.of("sonarlintMain"));

    // then the gradle build shall fail
    assertThat(buildResult.task(":sonarlintMain").getOutcome()).isEqualTo(TaskOutcome.FAILED);
    // and
    assertThat(buildResult.getOutput())
        .contains("Failed to load plugin 'Text Code Quality and Security' version 2.0.1.611.");

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
