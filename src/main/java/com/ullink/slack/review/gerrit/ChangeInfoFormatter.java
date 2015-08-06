package com.ullink.slack.review.gerrit;

import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackSession;

public interface ChangeInfoFormatter
{
    public SlackAttachment createAttachment(String changeId, ChangeInfo changeInfo, SlackSession session);
}
