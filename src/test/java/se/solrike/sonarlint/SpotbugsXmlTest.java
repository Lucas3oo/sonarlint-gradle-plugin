package se.solrike.sonarlint;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;

import se.solrike.sonarlint.impl.IssueEx;
import se.solrike.sonarlint.impl.SportbugsXmlBuilder;

public class SpotbugsXmlTest {

  @BeforeEach
  public void setup() {
  }

  @Test
  public void generateCorrectSpotbugsXml() {
    SportbugsXmlBuilder builder = new SportbugsXmlBuilder();

    List<IssueEx> issues = new ArrayList<>();
    IssueEx issue = mock(IssueEx.class);
    when(issue.getRuleKey()).thenReturn("java:S1220");
    when(issue.getSeverity()).thenReturn("CRITICAL");
    when(issue.getType()).thenReturn("VULNERABILITY");
    when(issue.getMessage()).thenReturn("Move this file to a named package.");
    when(issue.getStartLine()).thenReturn(31);
    when(issue.getEndLine()).thenReturn(83);
    when(issue.getFileName()).thenReturn("Hello.java");
    when(issue.getInputFileRelativePath()).thenReturn("src/main/java/Hello.java");
    StandaloneRuleDetails s = mock(StandaloneRuleDetails.class);
    when(s.getHtmlDescription()).thenReturn("Some long <b>html-ish</b> text");
    issue.setRulesDetails(Optional.of(s));

    issues.add(issue);

    File buildDir = new File("build");
    buildDir.mkdir();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter("build/sonarlint.xml", Charset.forName("UTF-8")))) {
      builder.generateIssues(writer, issues);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    System.out.println(buildDir.getAbsolutePath());

  }

//  @Test
//  public void generateXml() {
//    SportbugsXmlBuilder builder = new SportbugsXmlBuilder();
//
//    System.out.println(builder.generateXml());
//
//  }

}