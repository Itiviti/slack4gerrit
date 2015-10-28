package com.ullink.slack.review.gerrit;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.google.common.base.Function;
import com.google.common.net.UrlEscapers;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ullink.slack.review.Constants;
import com.ullink.slack.review.HttpHelper;
import com.ullink.slack.review.gerrit.ChangeInfo.IssuePriority;
import com.ullink.slack.review.gerrit.ChangeInfo.IssueType;

@Singleton
public class GerritChangeInfoService
{

    @Inject
    @Named(Constants.GERRIT_URL)
    private String gerritURL;

    @Inject
    @Named(Constants.JIRA_URL)
    private String jiraURL;

    @Inject
    @Named(Constants.JIRA_USER)
    private String jiraUser;

    @Inject
    @Named(Constants.JIRA_PASSWORD)
    private String jiraPassword;

    private static class JIRAParserFunction implements Function<String, JIRAInfo>
    {

        String jiraId;

        public JIRAParserFunction(String jiraId)
        {
            this.jiraId = jiraId;
        }

        @Override
        public JIRAInfo apply(String input)
        {
            JSONParser parser = new JSONParser();
            try
            {
                JSONObject obj = (JSONObject) parser.parse(input);
                JSONObject fieldsJSON = (JSONObject) obj.get("fields");
                JSONObject priorityJSON = (JSONObject) fieldsJSON.get("priority");
                String priority = (String) priorityJSON.get("name");
                JSONObject issueTypeJSON = (JSONObject) fieldsJSON.get("issuetype");
                String issueType = (String) issueTypeJSON.get("name");
                JIRAInfo toReturn = new JIRAInfo(IssuePriority.fromString(priority), IssueType.fromString(issueType), jiraId);
                return toReturn;
            }
            catch (ParseException e)
            {
                return null;
            }
        }

    }

    public ChangeInfo getChangeInfo(String changeId, boolean fetchJiraInfo) throws IOException
    {
        ListenableFuture<String> jsonChangeInfoHolder = HttpHelper.getAsyncFromHttp(new URL(gerritURL + "changes/" + changeId + "/detail"));
        ListenableFuture<String> jsonCommitMessageHolder = HttpHelper.getAsyncFromHttp(new URL(gerritURL + "changes/" + changeId + "/revisions/current/commit"));

        try
        {
            List<String> changeJSONInfos;
            try
            {
                changeJSONInfos = Futures.<String> successfulAsList(jsonChangeInfoHolder, jsonCommitMessageHolder).get(20000, TimeUnit.MILLISECONDS);
            }
            catch (Exception e)
            {
                // TODO improve exception handling
                e.printStackTrace();
                return null;
            }
            ChangeInfo changeInfo = new ChangeInfoJSONParser(changeJSONInfos.get(0), changeJSONInfos.get(1)).parse();

            String relatedChangesInfo = HttpHelper.getFromHttp(new URL(gerritURL + "changes/?q=change:" + changeInfo.getChangeId()));

            String cherryPickId = CherryPicksHelper.getCherryPickLegacyId(relatedChangesInfo);
            if (!changeId.equals(cherryPickId))
            {
                changeInfo.setCherryPickedFrom(cherryPickId);
            }

            if (changeInfo.getRelatedJira().size() > 0 && fetchJiraInfo)
            {

                List<ListenableFuture<JIRAInfo>> jiraInfoFutures = new ArrayList<ListenableFuture<JIRAInfo>>();
                for (Iterator<Entry<String, JIRAInfo>> entryIterator = changeInfo.getRelatedJira().entrySet().iterator(); entryIterator.hasNext();)
                {
                    Entry<String, JIRAInfo> entry = entryIterator.next();
                    jiraInfoFutures.add(Futures.transform(HttpHelper.getAsyncFromHttp(new URL(jiraURL + "rest/api/2/issue/" + entry.getKey()), jiraUser, jiraPassword), new JIRAParserFunction(entry.getKey())));
                }
                ListenableFuture<List<JIRAInfo>> jiraInfosFuture = Futures.<JIRAInfo> successfulAsList(jiraInfoFutures);
                List<JIRAInfo> jiraInfos;
                try
                {
                    jiraInfos = jiraInfosFuture.get(20000, TimeUnit.MILLISECONDS);
                }
                catch (Exception e)
                {
                    // TODO improve exception handling
                    e.printStackTrace();
                    return null;
                }
                for (JIRAInfo jiraInfo : jiraInfos)
                {
                    changeInfo.getRelatedJira().put(jiraInfo.getJiraId(), jiraInfo);
                }
            }
            return changeInfo;
        }
        catch (java.text.ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public boolean isMergedOrAbandoned(String changeId)
    {
        try
        {
            ListenableFuture<String> jsonChangeInfoHolder = HttpHelper.getAsyncFromHttp(new URL(gerritURL + "changes/" + changeId + "/detail"));
            String json = jsonChangeInfoHolder.get(20000, TimeUnit.MILLISECONDS);
            json = json.substring(4);
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(json);
            String status = (String) jsonObj.get("status");
            return "MERGED".equals(status) || "ABANDONED".equals(status);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public boolean projectExists(String projectId) throws IOException
    {
        ListenableFuture<String> jsonChangeInfoHolder = HttpHelper.getAsyncFromHttp(new URL(gerritURL + "projects/" + UrlEscapers.urlPathSegmentEscaper().escape(projectId)));
        try
        {
            String json = jsonChangeInfoHolder.get(20000, TimeUnit.MILLISECONDS);
            if ("NOT FOUND".equalsIgnoreCase(json.trim()))
            {
                return false;
            }
            json = json.substring(4);
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(json);
            String state = (String) jsonObj.get("state");
            return "ACTIVE".equals(state);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
}
