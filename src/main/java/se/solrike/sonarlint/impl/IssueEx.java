package se.solrike.sonarlint.impl;

import java.util.List;
import java.util.Optional;

import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.analysis.api.Flow;
import org.sonarsource.sonarlint.core.analysis.api.QuickFix;
import org.sonarsource.sonarlint.core.analysis.api.TextRange;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;

/**
 * @author Lucas Persson
 */
public class IssueEx implements Issue {

  private Issue mSonarlintIssue;
  private Optional<StandaloneRuleDetails> mRulesDetails;
  private final int mId;

  public IssueEx(int id, Issue sonarlintIssue) {
    mId = id;
    mSonarlintIssue = sonarlintIssue;
  }

  public int getId() {
    return mId;
  }

  @Override
  public TextRange getTextRange() {
    return mSonarlintIssue.getTextRange();
  }

  @Override
  public String getMessage() {
    return mSonarlintIssue.getMessage();
  }

  @Override
  public ClientInputFile getInputFile() {
    return mSonarlintIssue.getInputFile();
  }

  public String getInputFileRelativePath() {
    ClientInputFile inputFile = mSonarlintIssue.getInputFile();
    if (inputFile != null) {
      return inputFile.relativePath();
    }
    else {
      return "global";
    }
  }

  @Override
  public Integer getStartLine() {
    return mSonarlintIssue.getStartLine();
  }

  @Override
  public String getSeverity() {
    return mSonarlintIssue.getSeverity();
  }

  @Override
  public String getType() {
    return mSonarlintIssue.getType();
  }

  @Override
  public String getRuleKey() {
    return mSonarlintIssue.getRuleKey();
  }

  @Override
  public Integer getStartLineOffset() {
    return mSonarlintIssue.getStartLineOffset();
  }

  @Override
  public List<Flow> flows() {
    return mSonarlintIssue.flows();
  }

  @Override
  public List<QuickFix> quickFixes() {
    return mSonarlintIssue.quickFixes();
  }

  @Override
  public Integer getEndLine() {
    return mSonarlintIssue.getEndLine();
  }

  @Override
  public Integer getEndLineOffset() {
    return mSonarlintIssue.getEndLineOffset();
  }

  public Optional<StandaloneRuleDetails> getRulesDetails() {
    return mRulesDetails;
  }

  public void setRulesDetails(Optional<StandaloneRuleDetails> rulesDetails) {
    mRulesDetails = rulesDetails;
  }

  public String getFileName() {
    ClientInputFile inputFile = mSonarlintIssue.getInputFile();
    if (inputFile != null) {
      String path = inputFile.relativePath();
      return path.substring(path.lastIndexOf('/') + 1);
    }
    else {
      return "global";
    }
  }

}
