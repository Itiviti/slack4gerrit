package com.ullink.slack.review.gerrit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersLookupByEmailRequest;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import com.slack.api.model.User;
import com.ullink.slack.review.Constants;
import com.ullink.slack.review.gerrit.ChangeInfo.IssuePriority;

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
    public Attachment createAttachment(String changeId, ChangeInfo changeInfo, App app)
    {
        String subject = HtmlEscapers.htmlEscaper().escape(changeInfo.getSubject());
        String pretext = "*<" + gerritURL + changeId + "/" + "|" + subject + " (#" + changeId + ")>* *owner :* " + formatOwnerInfo(changeInfo, app);
        if (changeInfo.getCherryPickedFrom() != null)
        {
            pretext += " *Cherry picked from :* *<" + gerritURL + changeInfo.getCherryPickedFrom() + "/" + "| #" + changeInfo.getCherryPickedFrom() + ">*";
        }
        List<Field> fields = new ArrayList<>();
        fields.add(Field.builder()
            .value(formatProjectInfo(changeInfo) + " " + formatLastUpdatedInfo(changeInfo))
            .valueShortEnough(false)
            .build());
        String issuesDescription = formatRelatedIssues(changeInfo);
        if (issuesDescription != null)
        {
            fields.add(Field.builder()
                .value(issuesDescription)
                .valueShortEnough(true)
                .build());
        }
        List<String> markdownIn = new ArrayList<>();
        markdownIn.add("fields");
        markdownIn.add("pretext");
        return Attachment.builder()
            .mrkdwnIn(markdownIn)
            .pretext(pretext)
            .fields(fields).build();
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
        if (!Strings.isNullOrEmpty(jiraURL)) {
            builder.append('<').append(jiraURL).append(jiraData.getJiraId()).append('|').append(' ').append(jiraData.getJiraId()).append('>');
        }
    }

    protected String displayUser(User user)
    {
        String realName = user.getRealName();
        if (realName == null || realName.isEmpty())
        {
            return "<@" + user.getId() + ">";
        }
        return realName + " (<@" + user.getId() + ">)";
    }

    protected String formatOwnerInfo(ChangeInfo changeInfo, App app)
    {
        return "`" + displayUser(app, changeInfo.getOwnerEmail(), changeInfo.getOwner()) + "`";
    }

    protected String displayUser(App app, String email, String name)
    {
        UsersLookupByEmailRequest request = UsersLookupByEmailRequest.builder()
            .email(email)
            .build();
        try
        {
            UsersLookupByEmailResponse response = app.client().usersLookupByEmail(request);
            User user = response.getUser();
            if (user == null)
            {
                return name;
            }
            return displayUser(user);
        }
        catch (IOException | SlackApiException e)
        {
            //TODO
            //throw new RuntimeException(e);
            return name;
        }
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
