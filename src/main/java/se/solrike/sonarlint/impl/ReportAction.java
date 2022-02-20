package se.solrike.sonarlint.impl;

import static java.util.Map.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;

import se.solrike.sonarlint.Sonarlint;
import se.solrike.sonarlint.SonarlintReport;

/**
 * @author Lucas Persson
 */
public class ReportAction {

  protected final Project mProject;
  protected final Sonarlint mTask;
  protected final List<IssueEx> mIssues;
  protected Map<String, Render> mReportRenders;

  public ReportAction(Project project, Sonarlint task, List<IssueEx> issues) {
    mProject = project;
    mTask = task;
    mIssues = issues;
    mReportRenders = ofEntries(entry("text", this::writeTextReport), entry("html", this::writeHtmlReport));
  }

  @SuppressWarnings("all")
  public void report() {
    Map<String, SonarlintReport> reports = mTask.getReports().getAsMap();

    // generate reports
    reports.forEach((name, report) -> {
      if (report.getEnabled().getOrElse(false)) {
        RegularFile file = report.getOutputLocation().getOrElse(getDefaultReportOutputLocation(name).get());
        File parentDir = file.getAsFile().getParentFile();
        mProject.mkdir(parentDir);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAsFile()))) {
          mReportRenders.get(name).render(writer, mIssues);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  protected void writeTextReport(BufferedWriter writer, List<IssueEx> issues) throws IOException {
    for (IssueEx issue : issues) {
      writer.write(String.format("%n%s %s %s %s at: %s:%d:%d%n%n", getIssueTypeIcon(issue.getType()),
          getIssueSeverityIcon(issue.getSeverity()), issue.getRuleKey(), issue.getMessage(),
          issue.getInputFileRelativePath(), issue.getStartLine(), issue.getStartLineOffset()));
    }
  }

  protected void writeHtmlReport(BufferedWriter writer, List<IssueEx> issues) throws IOException {
    writer.write(getHtmlHeader());
    for (IssueEx issue : issues) {
      writer.write(String.format("<h1>%s (%s)</h1>%n", issue.getMessage(), issue.getRuleKey()));
      writer.write(String.format("<p>%s %s</p>%n", getIssueTypeIcon(issue.getType()),
          getIssueSeverityIcon(issue.getSeverity())));
      writer.write(String.format("<p>%s:%d:%d</p>%n", issue.getInputFileRelativePath(), issue.getStartLine(),
          issue.getStartLineOffset()));
      Optional<StandaloneRuleDetails> rulesDetails = issue.getRulesDetails();
      if (rulesDetails.isPresent()) {
        writer.write(rulesDetails.get().getHtmlDescription());
        writer.newLine();
        writer.newLine();
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

  // https://www.utf8-chartable.de/unicode-utf8-table.pl
  // https://www.fileformat.info/info/unicode/char/1f600/index.htm
  // @formatter:off
  private static final Map<String, String> sIssueTypeIcon = ofEntries(
      entry("BUG",           "\uD83D\uDE31 Bug  "), // FACE SCREAMING IN FEAR
      entry("CODE_SMELL",    "\uD83E\uDD22 Smell"), // NAUSEATED FACE
      entry("VULNERABILITY", "\uD83D\uDE08 Sec. ")  // SMILING FACE WITH HORNS
      );
  // @formatter:on

  public String getIssueTypeIcon(String issueType) {
    return sIssueTypeIcon.get(issueType);
  }

  // @formatter:off
  private static final Map<String, String> sIssueSeverityIcon = ofEntries(
      entry("BLOCKER",  "\uD83C\uDF2A  Block"), // Cloud With Tornado
      entry("CRITICAL", "\uD83C\uDF29  Crit."), // Cloud With Lightning
      entry("MAJOR",    "\uD83C\uDF28  Major️"), // Cloud With Snow
      entry("MINOR",    "\uD83C\uDF26  Minor"), // White Sun Behind Cloud With Rain
      entry("INFO",     "\uD83C\uDF24  Info ")  // White Sun With Small Cloud
      );
  // @formatter:on

  public String getIssueSeverityIcon(String issueSeverity) {
    return sIssueSeverityIcon.get(issueSeverity);
  }

  protected Provider<RegularFile> getDefaultReportOutputLocation(String reportName) {
    ProjectLayout layout = mProject.getLayout();
    Provider<String> filePath = mProject.provider(
        () -> new File(mTask.getReportsDir().get().getAsFile(), mTask.getName() + "." + reportName).getAbsolutePath());

    return layout.getProjectDirectory().file(filePath);
  }

  @FunctionalInterface
  public interface Render {
    void render(BufferedWriter t, List<IssueEx> u) throws IOException;
  }
}
