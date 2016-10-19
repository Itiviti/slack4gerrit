package com.ullink.slack.review.subscription;

import java.util.Collection;

public interface SubscriptionService
{
    Collection<String> getChannelsListeningToProject(String projectName);
    Collection<String> getChannelsListeningToUser(String userName);
    Collection<String> getChannelSubscriptions(String channelId);
    void subscribeOnProject(String projectName, String channelId);
    void unsubscribeOnProject(String projectName, String channelId);
    void subscribeOnUser(String userName, String channelId);
    void unsubscribeOnUser(String userName, String channelId);

}
