package com.ullink.slack.review.gerrit;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.common.html.HtmlEscapers;
import com.ullink.slack.review.Constants;
import com.ullink.slack.review.gerrit.ChangeInfo.IssuePriority;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

public class DefaultChangeInfoFormatter implements ChangeInfoFormatter
{

    protected final String gerritURL;
    protected final String jiraURL;

    public DefaultChangeInfoFormatter(Properties properties)
    {
        this.gerritURL = properties.getProperty(Constants.GERRIT_URL);
        this.jiraURL = properties.getProperty(Constants.JIRA_URL);
    }

    @Override
    public SlackAttachment createAttachment(String changeId, ChangeInfo changeInfo, SlackSession session)
    {
        SlackAttachment attachment = new SlackAttachment();
        attachment.addMarkdownIn("fields");
        attachment.addMarkdownIn("pretext");
        String subject = HtmlEscapers.htmlEscaper().escape(changeInfo.getSubject());
        attachment.pretext = "*<" + gerritURL + changeId + "/" + "|" + subject + " (#" + changeId + ")>* *owner :* " + formatOwnerInfo(changeInfo, session);
        if (changeInfo.getCherryPickedFrom() != null)
        {
            attachment.pretext += " *Cherry picked from :* *<" + gerritURL + changeInfo.getCherryPickedFrom() + "/" + "| #" + changeInfo.getCherryPickedFrom() + ">*";
        }

        attachment.addField(null, formatProjectInfo(changeInfo) + " " + formatLastUpdatedInfo(changeInfo), false);
        String issuesDescription = formatRelatedIssues(changeInfo);
        if (issuesDescription != null)
        {
            attachment.addField(null, issuesDescription, true);
        }
        return attachment;
    }

    protected String formatRelatedIssues(ChangeInfo changeInfo)
    {
        Map<String, JIRAInfo> relatedIssuesByIssue = changeInfo.getRelatedJira();
        if (relatedIssuesByIssue == null || relatedIssuesByIssue.isEmpty())
        {
            return null;
        }
        EnumMap<IssuePriority, List<JIRAInfo>> relatedIssuesByPriority = new EnumMap<>(IssuePriority.class);
        for (Entry<String, JIRAInfo> entry : relatedIssuesByIssue.entrySet())
        {
            List<JIRAInfo> priorityIssues = relatedIssuesByPriority.get(entry.getValue().getPriority());
            if (priorityIssues == null)
            {
                priorityIssues = new ArrayList<>();
                relatedIssuesByPriority.put(entry.getValue().getPriority(), priorityIssues);
            }
            priorityIssues.add(entry.getValue());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("*Issues :* ");
        boolean first = true;
        for (Entry<IssuePriority, List<JIRAInfo>> entry : relatedIssuesByPriority.entrySet())
        {
            for (JIRAInfo relatedIssue : entry.getValue())
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    builder.append(", ");
                }
                displayRelatedIssue(builder, relatedIssue);
            }
        }
        return builder.toString();
    }

    protected void displayRelatedIssue(StringBuilder builder, JIRAInfo jiraData)
    {
        builder.append('<').append(jiraURL).append(jiraData.getJiraId()).append('|').append(' ').append(jiraData.getJiraId()).append('>');
    }

    protected String displayUser(SlackUser user)
    {
        String realName = user.getUserName();
        if (realName == null || realName.isEmpty())
        {
            return "<@" + user.getId() + ">";
        }
        return realName + " (<@" + user.getId() + ">)";
    }

    protected String formatOwnerInfo(ChangeInfo changeInfo, SlackSession session)
    {
        return "`" + displayUser(session, changeInfo.getOwnerEmail(), changeInfo.getOwner()) + "`";
    }

    protected String displayUser(SlackSession session, String email, String name)
    {
        SlackUser user = session.findUserByEmail(email);
        if (user == null)
        {
            return name;
        }
        return displayUser(user);
    }

    protected String formatProjectInfo(ChangeInfo changeInfo)
    {
        return "*Project:* " + changeInfo.getProject() + ":" + "`" + changeInfo.getBranch() + "`";
    }

    protected String formatLastUpdatedInfo(ChangeInfo changeInfo)
    {
        return "*Updated:* _" + formatUpdatedField(changeInfo.getUpdated()) + "_";
    }

    protected String formatUpdatedField(Date updateDate)
    {
        Date now = new Date();
        long nbDaysFromNow = (now.getTime() - updateDate.getTime()) / (1000l * 60 * 60 * 24);
        if (nbDaysFromNow <= 0)
        {
            long nbHoursFromNow = (now.getTime() - updateDate.getTime()) / (1000l * 60 * 60);
            if (nbHoursFromNow <= 0)
            {
                long nbMinutesFromNow = (now.getTime() - updateDate.getTime()) / (1000l * 60);
                if (nbMinutesFromNow <= 0)
                {
                    return "Less than a minute ago";
                }
                else
                {
                    if (nbMinutesFromNow == 1)
                    {
                        return "One minute ago";
                    }
                    return nbMinutesFromNow + " minutes ago";
                }
            }
            else
            {
                if (nbHoursFromNow == 1)
                {
                    return "One hour ago";
                }
                return nbHoursFromNow + " hours ago";
            }
        }
        else
        {
            if (nbDaysFromNow == 1)
            {
                return "One day ago";
            }
            return nbDaysFromNow + " days ago";
        }
    }

}
