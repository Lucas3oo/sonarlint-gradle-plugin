package se.solrike.sonarlint.impl.util

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

  boolean getDownload(Project project) {
    Object extension = project.getExtensions().findByName("node");
    if (extension != null) {
      if (extension.hasProperty("download")) {
        return extension.download.getOrNull();
      }
    }
    return false;
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
//    if (os.contains("windows")) {
//      return "win";
//    }
    throw new RuntimeException("Unsupported OS: " + os)
  }

  String getArch() {
    String arch = System.getProperty("os.arch").toLowerCase();
    if (arch.contains("arm") || arch.contains("aarch64")) {
      return "arm64";
    }
    if (arch.contains("x86_64") || arch.contains("x64")) {
      return "x64";
    }
    throw new RuntimeException("Unsupported arch: " + arch)
  }

  // e.g.  <path_to_workDir>/node-v14.17.2-darwin-x64/bin/node
  Path getNodeExec(Project project) {
    String path = "${getNodeWorkDir(project)}/node-v${getNodeVersion(project)}-${getOs()}-${getArch()}/bin/node";
    return Paths.get(path)
  }
}