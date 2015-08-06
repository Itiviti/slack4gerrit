package com.ullink.slack.review.gerrit.reviewrequests;

import java.io.Serializable;

public class ReviewRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String            channelId;
    private String            changeId;
    private String            lastRequestTimestamp;

    public ReviewRequest(String lastRequestTimestamp, String changeId, String channelId)
    {
        this.channelId = channelId;
        this.changeId = changeId;
        this.lastRequestTimestamp = lastRequestTimestamp;
    }

    public String getChangeId()
    {
        return changeId;
    }

    public String getChannelId()
    {
        return channelId;
    }

    public String getLastRequestTimestamp()
    {
        return lastRequestTimestamp;
    }
}
