package com.ullink.slack.review.gerrit;

import com.slack.api.bolt.App;
import com.slack.api.model.Attachment;

public interface ChangeInfoFormatter
{
    Attachment createAttachment(String changeId, ChangeInfo changeInfo, App app);
}
