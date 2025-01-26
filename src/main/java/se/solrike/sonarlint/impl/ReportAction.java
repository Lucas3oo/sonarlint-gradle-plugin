package se.solrike.sonarlint.impl;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.ProviderFactory;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;

import se.solrike.sonarlint.Sonarlint;
import se.solrike.sonarlint.SonarlintReport;
import se.solrike.sonarlint.impl.util.SarifJsonBuilder;
import se.solrike.sonarlint.impl.util.SpotbugsXmlBuilder;

/**
 * @author Lucas Persson
 */
public class ReportAction {

  protected final Sonarlint mTask;
  protected final Logger mLogger;
  protected final ProjectLayout mLayout;
  protected final ProviderFactory mProviderFactory;
  protected Map<String, Render> mReportRenders;

  public ReportAction(Sonarlint task, Logger logger, ProjectLayout layout, ProviderFactory providerFactory) {
    mLogger = logger;
    mLayout = layout;
    mProviderFactory = providerFactory;
    mTask = task;
    mReportRenders = ofEntries(entry("text", this::renderTextReport), entry("html", this::renderHtmlReport),
        entry("xml", this::renderXmlReport), entry("sarif", this::renderSarifReport));
  }

  @SuppressWarnings("all")
  public void report(List<IssueEx> issues) {
    Map<String, SonarlintReport> reports = mTask.getReports().getAsMap();

    // generate reports
    reports.forEach((name, report) -> {
      if (report.getEnabled().getOrElse(Boolean.FALSE)) {
        RegularFile file = report.getOutputLocation().getOrElse(getDefaultReportOutputLocation(name));
        File parentDir = file.getAsFile().getParentFile();
        parentDir.mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAsFile(), Charset.forName("UTF-8")))) {
          mReportRenders.get(name).render(writer, issues);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
        mLogger.error("Report generated at: {}", file);
      }
    });
  }

  protected void renderTextReport(Writer writer, Iterable<IssueEx> issues) throws IOException {
    for (IssueEx issue : issues) {
      writer.write(String.format("%n%s %s %s %s at: %s:%d:%d%n%n", getIssueTypeIcon(issue.getType()),
          getIssueSeverityIcon(issue.getSeverity()), issue.getRuleKey(), issue.getMessage(),
          issue.getInputFileRelativePath(), issue.getStartLine(), issue.getStartLineOffset()));
    }
  }

  protected void renderHtmlReport(Writer writer, Collection<IssueEx> issues) throws IOException {
    writer.write(getHtmlHeader());

    // summary
    writer.write("<h1>Summary</h1>\n");
    writer.write("<list>\n");
    // type, count
    Map<RuleType, Long> issueCountPerType = issues.stream()
        .collect(Collectors.groupingBy(IssueEx::getType, Collectors.counting()));
    for (Entry<RuleType, Long> issueType : issueCountPerType.entrySet()) {
      writer.write(String.format("<li>%s: %d</li>%n", getIssueTypeIcon(issueType.getKey()), issueType.getValue()));
    }
    writer.write("</list>\n");

    // TOC
    writer.write("<h1>TOC</h1>\n" + "<list>\n");
    for (IssueEx issue : issues) {
      writer.write(String.format("<li>%s %s, <a href=\"#%d\">%s (%s)</a> at %s</li>%n",
          getIssueTypeIcon(issue.getType()), getIssueSeverityIcon(issue.getSeverity()), issue.getId(),
          StringEscapeUtils.escapeHtml4(issue.getMessage()), issue.getRuleKey(), issue.getFileName()));
    }
    writer.write("</list>\n");

    // all issues
    for (IssueEx issue : issues) {
      writer.write(String.format("<h1 id=\"%d\">%s (%s)</h1>%n", issue.getId(),
          StringEscapeUtils.escapeHtml4(issue.getMessage()), issue.getRuleKey()));
      writer.write(String.format("<p>%s %s</p>%n", getIssueTypeIcon(issue.getType()),
          getIssueSeverityIcon(issue.getSeverity())));
      writer.write(String.format("<p>%s:%d:%d</p>%n", issue.getInputFileRelativePath(), issue.getStartLine(),
          issue.getStartLineOffset()));
      Optional<StandaloneRuleDetails> rulesDetails = issue.getRulesDetails();
      if (rulesDetails.isPresent()) {
        writer.write(rulesDetails.get().getHtmlDescription());
        writer.write("\n");
        writer.write("\n");
      }
    }
    writer.write("</body>\n</html>");
  }

  protected String getHtmlHeader() {
    // @formatter:off
    return "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
        + "<html lang=\"en\">\n"
        + "<head>\n"
        + "  <meta charset=\"UTF-8\">\n"
        + "  <title>SonarLint analysis report</title>\n"
        + "  <style type=\"text/css\">\n"
        + "    h1 {\n"
        + "      background-color: LightGray;\n"
        + "    }\n"
        + "  </style>\n"
        + "</head>\n"
        + "<body>\n";
    // @formatter:on
  }

  protected void renderXmlReport(Writer writer, Collection<IssueEx> issues) {
    new SpotbugsXmlBuilder().generateBugCollection(writer, issues, Set.of(mLayout.getProjectDirectory().getAsFile()));
  }

  protected void renderSarifReport(Writer writer, Collection<IssueEx> issues) {
    new SarifJsonBuilder().generateBugCollection(writer, issues, mLayout.getProjectDirectory().getAsFile());
  }

  // https://www.utf8-chartable.de/unicode-utf8-table.pl
  // https://www.fileformat.info/info/unicode/char/1f600/index.htm
  // @formatter:off
  //CHECKSTYLE:OFF
  private static final Map<RuleType, String> sIssueTypeIcon = ofEntries(
      entry(RuleType.BUG,           "\uD83D\uDE31 Bug  "), // FACE SCREAMING IN FEAR
      entry(RuleType.CODE_SMELL,    "\uD83E\uDD22 Smell"), // NAUSEATED FACE
      entry(RuleType.VULNERABILITY, "\uD83D\uDE08 Sec. "),  // SMILING FACE WITH HORNS
      // Needs review, but I don't think sonarlint will ever report this. Only SonarCloude.
      entry(RuleType.SECURITY_HOTSPOT, "Review ")
      );
  // @formatter:on
  // CHECKSTYLE:ON

  public String getIssueTypeIcon(RuleType ruleType) {
    return sIssueTypeIcon.get(ruleType);
  }

  // @formatter:off
  //CHECKSTYLE:OFF
  private static final Map<IssueSeverity, String> sIssueSeverityIcon = ofEntries(
      entry(IssueSeverity.BLOCKER,  "\uD83C\uDF2A  Block"), // Cloud With Tornado
      entry(IssueSeverity.CRITICAL, "\uD83C\uDF29  Crit."), // Cloud With Lightning
      entry(IssueSeverity.MAJOR,    "\uD83C\uDF28  Major"), // Cloud With Snow
      entry(IssueSeverity.MINOR,    "\uD83C\uDF26  Minor"), // White Sun Behind Cloud With Rain
      entry(IssueSeverity.INFO,     "\uD83C\uDF24  Info ")  // White Sun With Small Cloud
      );
  // @formatter:on
  // CHECKSTYLE:ON

  public String getIssueSeverityIcon(IssueSeverity issueSeverity) {
    return sIssueSeverityIcon.get(issueSeverity);
  }

  protected RegularFile getDefaultReportOutputLocation(String reportName) {
    File file = new File(mTask.getReportsDir().get().getAsFile(), mTask.getName() + "." + reportName);
    String filePath = file.getAbsolutePath();
    return mLayout.getProjectDirectory().file(filePath);
  }

  @FunctionalInterface
  public interface Render {
    void render(Writer writer, List<IssueEx> issues) throws IOException;
  }
}
