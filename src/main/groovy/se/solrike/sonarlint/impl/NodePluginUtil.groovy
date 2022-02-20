package se.solrike.sonarlint.impl

import java.nio.file.Path
import java.nio.file.Paths

import org.gradle.api.Project
import org.gradle.api.file.Directory

public class NodePluginUtil {

  String getNodeVersion(Project project) {
    Object extension = project.getExtensions().findByName("node");
    if (extension != null) {
      if (extension.hasProperty("version")) {
        return extension.version.getOrNull();
      }
    }
    return null;
  }

  Directory getNodeWorkDir(Project project) {
    Object extension = project.getExtensions().findByName("node");
    if (extension != null) {
      if (extension.hasProperty("workDir")) {
        return extension.workDir.getOrNull();
      }
    }
    return null;
  }


  String getOs() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("mac")) {
      return "darwin";
    }
    if (os.contains("linux")) {
      return "linux";
    }
    throw new RuntimeException("Unsupported OS: " + os)
  }

  String getArch() {
    String arch = System.getProperty("os.arch").toLowerCase();
    if (arch.contains("x86_64")) {
      return "x64";
    }
    if (arch.contains("arm")) {
      return "arm64";
    }
    throw new RuntimeException("Unsupported arch: " + arch)
  }

  // e.g.  <path_to_workDir>/node-v14.17.2-darwin-x64/bin/node
  Path getNodeExec(Project project) {
    String path = "${getNodeWorkDir(project)}/node-v${getNodeVersion(project)}-${getOs()}-${getArch()}/bin/node";
    return Paths.get(path)
  }
}