package com.ullink.slack.review.gerrit;

import com.ullink.slack.review.gerrit.ChangeInfo.IssuePriority;
import com.ullink.slack.review.gerrit.ChangeInfo.IssueType;

public class JIRAInfo
{
    private IssuePriority priority;
    private IssueType issueType;
    private String jiraId;

    public JIRAInfo(IssuePriority priority, IssueType issueType, String jiraId)
    {
        this.priority = priority;
        this.issueType = issueType;
        this.jiraId = jiraId;
    }

    public IssueType getIssueType()
    {
        return issueType;
    }

    public String getJiraId()
    {
        return jiraId;
    }

    public IssuePriority getPriority()
    {
        return priority;
    }

    public void setIssueType(IssueType issueType)
    {
        this.issueType = issueType;
    }

    public void setJiraId(String jiraId)
    {
        this.jiraId = jiraId;
    }

    public void setPriority(IssuePriority priority)
    {
        this.priority = priority;
    }

}
