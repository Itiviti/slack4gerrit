package com.ullink.slack.review.subscription;

import java.util.Collection;

public interface ProjectSubscriptionService
{
    Collection<String> getListeningChannels(String projectName);
    Collection<String> getChannelSubscriptions(String channelId);
    void subscribeOnProject(String projectName, String channelId);
    void unsubscribeOnProject(String projectName, String channelId);

}
