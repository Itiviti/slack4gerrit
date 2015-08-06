package com.ullink.slack.review.gerrit;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        JSONParser parser = new JSONParser();
        JSONObject obj = null;

        try
        {
            obj = (JSONObject) parser.parse(changeInfoJSON);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        String project = (String) obj.get("project");
        String branch = (String) obj.get("branch");
        String subject = (String) obj.get("subject");
        String created = (String) obj.get("created");
        String updated = (String) obj.get("updated");
        String changeId = (String) obj.get("change_id");
        String id = (String) obj.get("id");
        Long insertion = (Long) obj.get("insertions");
        Long deletion = (Long) obj.get("deletions");

        JSONObject ownerJSON = (JSONObject) obj.get("owner");
        String ownerName = (String) ownerJSON.get("name");
        String ownerEmail = (String) ownerJSON.get("email");

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

        JSONObject labelsJSON = (JSONObject) obj.get("labels");
        for (Object label : labelsJSON.keySet())
        {
            String labelName = (String) label;
            Set<Review> reviewSet = new HashSet<Review>();
            changeInfo.getReviews().put(labelName, reviewSet);
            JSONObject reviewGroupJSON = (JSONObject) labelsJSON.get(labelName);
            JSONArray allReviewsJSONArray = (JSONArray) reviewGroupJSON.get("all");

            if (allReviewsJSONArray != null)
            {
                for (Object r : allReviewsJSONArray)
                {
                    JSONObject reviewJSON = (JSONObject) r;
                    Long value = (Long) reviewJSON.get("value");
                    if (value != null && value.intValue() != 0)
                    { // if value is null, no review for this person
                        String name = (String) reviewJSON.get("name");
                        try
                        {
                            name = new String(name.getBytes(),"UTF-8");
                        }
                        catch (UnsupportedEncodingException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        String email = (String) reviewJSON.get("email");
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
        parser = new JSONParser();
        try
        {
            obj = (JSONObject) parser.parse(commitMessageJSON);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        String message = (String) obj.get("message");
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
