package se.solrike.sonarlint;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Paths;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SonarlintPluginTest {

  private Project mProject;

  @BeforeEach
  public void setup() {
    mProject = ProjectBuilder.builder().build();
    mProject.getPluginManager().apply(JavaBasePlugin.class);
    mProject.getPluginManager().apply(SonarlintPlugin.class);
  }

  @Test
  public void projectHasConfiguration() {
    Configuration configuration = mProject.getConfigurations().findByName(SonarlintPlugin.CONFIG_NAME);
    assertThat(configuration).isNotNull();

    configuration = mProject.getConfigurations().findByName(SonarlintPlugin.PLUGINS_CONFIG_NAME);
    assertThat(configuration).isNotNull();
  }

  @Test
  public void projectHasExtension() {
    Object extension = mProject.getExtensions().findByName(SonarlintPlugin.EXTENSION_NAME);

    assertThat(extension).isNotNull();
  }

  @Test
  public void sonarlintTaskExecutes() {
    mProject.getTasks().create("mySonarLint", Sonarlint.class);
    Sonarlint task = (Sonarlint) mProject.getTasks().getByName("mySonarLint");
    task.setSource(mProject.fileTree(Paths.get("./src/main/java")));
    task.setCompileClasspath(mProject.fileTree(Paths.get("./lib")));
    task.setClassFiles(mProject.fileTree(Paths.get("./build/classes/java/main")));

    task.run();
  }

}