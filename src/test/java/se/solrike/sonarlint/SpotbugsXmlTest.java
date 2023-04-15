package se.solrike.sonarlint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;

import se.solrike.sonarlint.impl.IssueEx;
import se.solrike.sonarlint.impl.util.SpotbugsXmlBuilder;

class SpotbugsXmlTest {

  @Test
  void generateCorrectSpotbugsXml() {
    SpotbugsXmlBuilder builder = new SpotbugsXmlBuilder();

    List<IssueEx> issues = new ArrayList<>();
    IssueEx issue = mock(IssueEx.class);
    when(issue.getRuleKey()).thenReturn("java:S1220");
    when(issue.getSeverity()).thenReturn("CRITICAL");
    when(issue.getType()).thenReturn("VULNERABILITY");
    when(issue.getMessage()).thenReturn("Move this file to a named <i>package</i>.");
    when(issue.getStartLine()).thenReturn(31);
    when(issue.getEndLine()).thenReturn(83);
    when(issue.getFileName()).thenReturn("Sonarlint.java");
    when(issue.getInputFileRelativePath()).thenReturn("se/solrike/sonarlint/Sonarlint.java");
    StandaloneRuleDetails s = mock(StandaloneRuleDetails.class);
    when(s.getHtmlDescription()).thenReturn("Some long <b>html-ish</b> text");
    when(issue.getRulesDetails()).thenReturn(Optional.of(s));
    issues.add(issue);
    issues.add(issue);

    StringWriter writer = new StringWriter();

    builder.generateBugCollection(writer, issues,
        Set.of(new File("/home/runner/work/sonarlint-gradle-plugin/sonarlint-gradle-plugin/src/main/java")));

    assertThat(writer.toString()).contains("java:S1220", "SECURITY", "Sonarlint.java");

  }

}