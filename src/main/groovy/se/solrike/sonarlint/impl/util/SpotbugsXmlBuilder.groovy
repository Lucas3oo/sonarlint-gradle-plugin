package se.solrike.sonarlint.impl.util

import java.util.stream.Collectors

import groovy.xml.MarkupBuilder
import se.solrike.sonarlint.impl.BugPattern
import se.solrike.sonarlint.impl.IssueEx

class SpotbugsXmlBuilder {

  private static final Map<String, Integer> sIssueSeverityToRank = ['BLOCKER':1, 'CRITICAL':3, 'MAJOR':5, 'MINOR':7, 'INFO':20]
  private static final Map<String, String> sIssueTypeToCategory = ['BUG':'CORRECTNESS', 'CODE_SMELL':'STYLE', 'VULNERABILITY':'SECURITY']

  public SpotbugsXmlBuilder() {
    super()
  }

  public Writer generateBugCollection(Writer writer, Collection<IssueEx> issues, Set<File> srcDirs) {
    Collection<BugPattern> bugPatters = getBugPatters(issues);
    MarkupBuilder builder = new MarkupBuilder(writer)
    builder.mkp.xmlDeclaration([version:'1.0',encoding:'UTF-8'])
    builder.BugCollection {
      Project {
        srcDirs.each { srcDir ->
          SrcDir (srcDir.absolutePath)
        }
      }
      issues.each { issue ->
        builder.BugInstance (type: issue.ruleKey, priority: 1, rank: sIssueSeverityToRank.get(issue.severity), category: sIssueTypeToCategory.get(issue.type), instanceHash: issue.id) {
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


  private Collection<BugPattern> getBugPatters(Collection<IssueEx> issues) {
    return issues.stream().map({ issue ->
      new BugPattern(issue.ruleKey, sIssueTypeToCategory.get(issue.type), issue.message,
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
// Category - The category is for grouping BugPatterns, one of:
//   BAD_PRACTICE CORRECTNESS EXPERIMENTAL I18N MALICIOUS_CODE MT_CORRECTNESS NOISE PERFORMANCE SECURITY STYLE(dodgy code)

// Sonarlint:
// Rule key - ID for the pattern/issue. e.g java:s1234
// Severity - one of BLOCKER CRITICAL MAJOR MINOR INFO
// Type - one of BUG CODE_SMELL VULNERABILITY

