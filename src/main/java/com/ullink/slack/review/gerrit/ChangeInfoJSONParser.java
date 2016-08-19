package com.ullink.slack.review.gerrit;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.GsonHelper;

public class ChangeInfoJSONParser
{

    private static final Logger LOGGER       = LoggerFactory.getLogger(ChangeInfoJSONParser.class);

    private String              DATE_PATTERN = "yyyy-MM-dd hh:mm:ss.SSSS";

    private String              changeInfoJSON;
    private String              commitMessageJSON;

    ChangeInfoJSONParser(String changeInfoJSON, String commitMessageJSON)
    {
        this.changeInfoJSON = changeInfoJSON.substring(4);
        this.commitMessageJSON = commitMessageJSON.substring(4);
    }

    ChangeInfo parse() throws java.text.ParseException
    {
        LOGGER.debug("parsing ChangeInfo : " + changeInfoJSON);
        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(changeInfoJSON).getAsJsonObject();

        String project = GsonHelper.getStringOrNull(obj.get("project"));
        String branch = GsonHelper.getStringOrNull(obj.get("branch"));
        String subject = GsonHelper.getStringOrNull(obj.get("subject"));
        String created = GsonHelper.getStringOrNull(obj.get("created"));
        String updated = GsonHelper.getStringOrNull(obj.get("updated"));
        String changeId = GsonHelper.getStringOrNull(obj.get("change_id"));
        String id = GsonHelper.getStringOrNull(obj.get("id"));
        Long insertion = GsonHelper.getLongOrNull(obj.get("insertions"));
        Long deletion = GsonHelper.getLongOrNull(obj.get("deletions"));

        JsonObject ownerJSON = obj.get("owner").getAsJsonObject();
        String ownerName = GsonHelper.getStringOrNull(ownerJSON.get("name"));
        String ownerEmail = GsonHelper.getStringOrNull(ownerJSON.get("email"));

        ChangeInfo changeInfo = new ChangeInfo();
        changeInfo.setBranch(branch);
        changeInfo.setCreated(new SimpleDateFormat(DATE_PATTERN).parse(created.substring(0, created.length() - 4)));
        changeInfo.setDeletion(deletion.intValue());
        changeInfo.setInsertion(insertion.intValue());
        changeInfo.setUpdated(new SimpleDateFormat(DATE_PATTERN).parse(updated.substring(0, updated.length() - 4)));
        changeInfo.setProject(project);
        changeInfo.setSubject(subject);
        changeInfo.setOwner(ownerName);
        changeInfo.setOwnerEmail(ownerEmail);
        changeInfo.setChangeId(changeId);
        changeInfo.setId(id);

        // collecting reviews value

        JsonObject labelsJSON = obj.get("labels").getAsJsonObject();
        for (Map.Entry<String,JsonElement> labelEntry : labelsJSON.entrySet())
        {
            String labelName = labelEntry.getKey();
            Set<Review> reviewSet = new HashSet<Review>();
            changeInfo.getReviews().put(labelName, reviewSet);
            JsonObject reviewGroupJSON = labelEntry.getValue().getAsJsonObject();
            JsonArray allReviewsJSONArray = GsonHelper.getJsonArrayOrNull(reviewGroupJSON.get("all"));

            if (allReviewsJSONArray != null)
            {
                for (JsonElement reviewElement : allReviewsJSONArray)
                {
                    JsonObject reviewJSON = reviewElement.getAsJsonObject();
                    Long value = GsonHelper.getLongOrNull(reviewJSON.get("value"));
                    if (value != null && value.intValue() != 0)
                    { // if value is null, no review for this person
                        String name = GsonHelper.getStringOrNull(reviewJSON.get("name"));
                        try
                        {
                            name = new String(name.getBytes(),"UTF-8");
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        String email = GsonHelper.getStringOrNull(reviewJSON.get("email"));
                        Review review = new Review();
                        review.setReviewer(name);
                        review.setReviewValue(value.intValue());
                        review.setReviewerEmail(email);
                        reviewSet.add(review);
                    }
                }

            }
        }

        // extracting commit message
        LOGGER.debug("parsing Commit message info : " + commitMessageJSON);
        parser = new JsonParser();
        obj = parser.parse(commitMessageJSON).getAsJsonObject();
        String message = GsonHelper.getStringOrNull(obj.get("message"));
        changeInfo.setCommitMessage(message);
        // getting the JIRA
        int jiraPos = message.indexOf("Issue:");
        if (jiraPos >= 0)
        {
            int endJiraPos = message.indexOf('\n', jiraPos);
            String jiraList = message.substring(jiraPos + 6, endJiraPos);
            String[] jiras = jiraList.split(",");
            for (String jira : jiras)
            {
                changeInfo.getRelatedJira().put(jira.trim(), null);
            }

        }
        return changeInfo;
    }

}
