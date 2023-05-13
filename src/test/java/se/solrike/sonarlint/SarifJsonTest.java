package se.solrike.sonarlint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;
import org.sonarsource.sonarlint.core.commons.Language;

import se.solrike.sonarlint.impl.IssueEx;
import se.solrike.sonarlint.impl.util.SarifJsonBuilder;

class SarifJsonTest {

  @Test
  void generateCorrectSarifJson() {
    SarifJsonBuilder builder = new SarifJsonBuilder();

    List<IssueEx> issues = new ArrayList<>();
    IssueEx issue = mock(IssueEx.class);
    when(issue.getRuleKey()).thenReturn("java:S1220");
    when(issue.getSeverity()).thenReturn("CRITICAL");
    when(issue.getType()).thenReturn("VULNERABILITY");
    when(issue.getMessage()).thenReturn("The default unnamed package should not be used");
    when(issue.getStartLine()).thenReturn(31);
    when(issue.getEndLine()).thenReturn(83);
    when(issue.getFileName()).thenReturn("Sonarlint.java");
    when(issue.getInputFileRelativePath()).thenReturn("src/main/java/se/solrike/sonarlint/Sonarlint.java");
    StandaloneRuleDetails s = mock(StandaloneRuleDetails.class);
    when(s.getHtmlDescription()).thenReturn("Some long <b>html-ish</b> text <pre>Do this and this</pre>");
    when(s.getLanguage()).thenReturn(Language.JAVA);
    when(s.getTags()).thenReturn(new String[] { "convention", "clumsy" });
    when(issue.getRulesDetails()).thenReturn(Optional.of(s));
    issues.add(issue);
    issues.add(issue);

    StringWriter writer = new StringWriter();

    builder.generateBugCollection(writer, issues,
        new File("/home/runner/work/sonarlint-gradle-plugin/sonarlint-gradle-plugin"));

    assertThat(writer.toString()).contains("java:S1220", "error", "Sonarlint.java",
        "Some long **html-ish** text\\n\\n```\\nDo this and this\\n```", "clumsy");

  }

}