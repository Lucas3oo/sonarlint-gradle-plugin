package se.solrike.sonarlint.impl;

import java.util.LinkedList;
import java.util.List;

import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;

/**
 * @author Lucas Persson
 */
public class IssueCollector implements IssueListener {
  private List<IssueEx> mIssues = new LinkedList<>();
  private int mNextId;

  @Override
  public void handle(Issue issue) {
    mIssues.add(new IssueEx(mNextId++, issue));
  }

  public List<IssueEx> getIssues() {
    return mIssues;
  }
}