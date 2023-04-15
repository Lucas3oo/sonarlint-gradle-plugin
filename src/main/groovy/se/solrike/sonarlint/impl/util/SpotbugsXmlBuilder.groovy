package se.solrike.sonarlint.impl.util

import java.util.stream.Collectors

import groovy.xml.MarkupBuilder
import se.solrike.sonarlint.impl.BugPattern
import se.solrike.sonarlint.impl.IssueEx

class SpotbugsXmlBuilder {

  public SpotbugsXmlBuilder() {
    super()
  }

  public Writer generateIssues(Writer writer, Collection<IssueEx> issues, Collection<String> srcDirs) {
    Collection<BugPattern> bugPatters = getBugPatters(issues);
    MarkupBuilder builder = new MarkupBuilder(writer)
    builder.mkp.xmlDeclaration([version:'1.0',encoding:'UTF-8'])
    builder.BugCollection {
      Project {
        srcDirs.each { srcDir ->
          SrcDir (srcDir)
        }
      }
      issues.each { issue ->
        builder.BugInstance (type: issue.ruleKey, priority: 1, rank: getIssueSeverityToRank(issue.severity), category: issue.type, instanceHash: issue.id) {
          ShortMessage {
            mkp.yieldUnescaped('<![CDATA[' + issue.message + ']]>')
          }
          LongMessage {
            mkp.yieldUnescaped('<![CDATA[' + issue.message + ']]>')
          }
          Class (classname: getClassname(issue.inputFileRelativePath), primary: 'true') {
            SourceLine (classname: getClassname(issue.inputFileRelativePath), start: issue.startLine, end: issue.endLine, sourcefile: issue.fileName, sourcepath: issue.inputFileRelativePath)
            Message ('In class ' + getClassname(issue.inputFileRelativePath))
          }
          SourceLine (classname: getClassname(issue.inputFileRelativePath), start: issue.startLine, end: issue.endLine, sourcefile: issue.fileName, sourcepath: issue.inputFileRelativePath)
        }
      }

      bugPatters.each { bugPattern ->
        BugPattern (type: bugPattern.type, category: bugPattern.category) {
          ShortMessage (bugPattern.shortDescription)
          Details {
            mkp.yieldUnescaped('<![CDATA[' + bugPattern.details + ']]>')
          }
        }
      }
    }

    return writer
  }

  private static final Map<String, Integer> sIssueSeverityToRank = ['BLOCKER':1, 'CRITICAL':3, 'MAJOR':5, 'MINOR':7, 'INFO':20]

  private Integer getIssueSeverityToRank(String issueSeverity) {
    return sIssueSeverityToRank.get(issueSeverity);
  }

  private Collection<BugPattern> getBugPatters(Collection<IssueEx> issues) {
    return issues.stream().map({ issue ->
      new BugPattern(issue.ruleKey, issue.type, issue.message,
          issue.rulesDetails.map({rd -> rd.htmlDescription}).orElse(issue.message))
    }).collect(Collectors.toSet())
  }

  private String getClassname(String filePath) {
    char dot = '.'
    return filePath.replace(File.separatorChar, dot).replace('.java', '')
  }
}


// Spotbugs:
// Type - This is just the name of the BugPattern which was found.
// Rank - indicates the severity 20-1 (1 of highest severity)
// Priority - It indicates the confidence that the found bug is actually a bug.  It varies from 1 (highest confidence) to 5 (lowest confidence, to be disregarded)
// Category - The category is for grouping BugPatterns, e.g.  CORRECTNESS
