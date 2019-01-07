package com.ullink.slack.review.gerrit;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    private static final Logger LOGGER       = LoggerFactory.getLogger(ChangeInfoJSONParser.class);

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
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(input).getAsJsonObject();
            JsonObject fieldsJSON = obj.get("fields").getAsJsonObject();
            JsonElement priorityNameElement = fieldsJSON.get("priority").getAsJsonObject().get("name");
            String priority = priorityNameElement != null ? priorityNameElement.getAsString() : null;
            JsonElement issueTypeNameElement = fieldsJSON.get("issuetype").getAsJsonObject().get("name");
            String issueType = issueTypeNameElement != null ? issueTypeNameElement.getAsString() : null;
            return new JIRAInfo(IssuePriority.fromString(priority), IssueType.fromString(issueType), jiraId);
        }

    }

    public ChangeInfo getChangeInfo(String changeId) throws IOException
    {
        ListenableFuture<String> jsonChangeInfoHolder = HttpHelper.getAsyncFromHttp(new URL(gerritURL + "changes/" + changeId + "/detail"));
        ListenableFuture<String> jsonCommitMessageHolder = HttpHelper.getAsyncFromHttp(new URL(gerritURL + "changes/" + changeId + "/revisions/current/commit"));

        try
        {
            List<String> changeJSONInfos;
            try
            {
                changeJSONInfos = Futures.successfulAsList(jsonChangeInfoHolder, jsonCommitMessageHolder).get(20000, TimeUnit.MILLISECONDS);
            }
            catch (Exception e)
            {
                LOGGER.error("Exception raised while getting ChangeInfo ", e);
                return null;
            }
            ChangeInfo changeInfo = new ChangeInfoJSONParser(changeJSONInfos.get(0), changeJSONInfos.get(1)).parse();

            String relatedChangesInfo = HttpHelper.getFromHttp(new URL(gerritURL + "changes/?q=change:" + changeInfo.getChangeId()));

            String cherryPickId = CherryPicksHelper.getCherryPickLegacyId(relatedChangesInfo);
            if (!changeId.equals(cherryPickId))
            {
                changeInfo.setCherryPickedFrom(cherryPickId);
            }

            if (changeInfo.getRelatedJira().size() > 0)
            {

                List<ListenableFuture<JIRAInfo>> jiraInfoFutures = new ArrayList<>();
                for (Iterator<Entry<String, JIRAInfo>> entryIterator = changeInfo.getRelatedJira().entrySet().iterator(); entryIterator.hasNext();)
                {
                    Entry<String, JIRAInfo> entry = entryIterator.next();
                    jiraInfoFutures.add(Futures.transform(HttpHelper.getAsyncFromHttp(new URL(jiraURL + "rest/api/2/issue/" + entry.getKey()), jiraUser, jiraPassword), new JIRAParserFunction(entry.getKey())));
                }
                ListenableFuture<List<JIRAInfo>> jiraInfosFuture = Futures.successfulAsList(jiraInfoFutures);
                List<JIRAInfo> jiraInfos;
                try
                {
                    jiraInfos = jiraInfosFuture.get(20000, TimeUnit.MILLISECONDS);
                }
                catch (Exception e)
                {
                    LOGGER.error("Exception raised while getting ChangeInfo ", e);
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
            LOGGER.error("Exception raised while parsing data ", e);
            return null;
        }
    }

    public boolean isMergedOrAbandoned(String changeId)
    {
        URL changeURL = buildURL(gerritURL + "changes/" + changeId + "/detail");
        String json = fetchJSONData(changeURL);
        if ("not found".equalsIgnoreCase(json.substring(0,9))) {
            return true;
        }
        JsonObject jsonObj = parseJson(json);
        JsonElement statusElement = jsonObj.get("status");
        String status = statusElement != null ? statusElement.getAsString() : null;
        return "MERGED".equals(status) || "ABANDONED".equals(status);
    }

    private JsonObject parseJson(String json)
    {
        try {
            JsonParser parser = new JsonParser();
            return parser.parse(json).getAsJsonObject();
        }
        catch (Exception e)
        {
            LOGGER.error("Exception raised while parsing JSON data from gerrit " + json + " ", e);
            throw new RuntimeException(e);
        }
    }

    private String fetchJSONData(URL changeURL)
    {
        try
        {
            ListenableFuture<String> jsonChangeInfoHolder = HttpHelper.getAsyncFromHttp(changeURL);
            return jsonChangeInfoHolder.get(20000, TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
            LOGGER.error("Exception raised while fetching data from gerrit location " + changeURL + " ", e);
            throw new RuntimeException(e);
        }
    }

    private URL buildURL(String urlPath)
    {
        try
        {
            return new URL(urlPath);
        }
        catch (MalformedURLException e)
        {
            LOGGER.error("Malformed URL Exception: ", e);
            throw new RuntimeException(e);
        }
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
            JsonParser parser = new JsonParser();
            JsonObject jsonObj = parser.parse(json).getAsJsonObject();
            JsonElement stateElement = jsonObj.get("state");
            String state = stateElement != null ? stateElement.getAsString() : null;
            return "ACTIVE".equals(state);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public boolean userExists(String userId) throws IOException
    {
        ListenableFuture<String> jsonChangeInfoHolder = HttpHelper.getAsyncFromHttp(new URL(gerritURL + "accounts/" + UrlEscapers.urlPathSegmentEscaper().escape(userId)));
        try
        {
            String json = jsonChangeInfoHolder.get(20000, TimeUnit.MILLISECONDS);
            if ("NOT FOUND".equalsIgnoreCase(json.trim()))
            {
                return false;
            }

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
}
