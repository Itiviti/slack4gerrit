package commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import com.ullink.slack.review.gerrit.ChangeInfo;
import com.ullink.slack.review.gerrit.ChangeInfoFormatter;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequest;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.review.subscription.SubscriptionService;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackBot;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackMessageHandle;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackChatConfiguration;
import com.ullink.slack.simpleslackapi.replies.SlackMessageReply;

class MessageHandler implements Runnable
{
    private SlackChannel                     fromChannel;
    private SlackChannel                     targetChannel;
    private String                           targetChannelName;
    private String                           changeId;
    private String                           comment = "";
    private final SlackSession               session;
    private final ReviewRequestService       reviewRequestService;
    private final SubscriptionService subscriptionService;
    private final GerritChangeInfoService    gerritChangeInfoService;
    private final ChangeInfoFormatter        changeInfoDecorator;

    MessageHandler(SlackChannel fromChannel, String changeId, String comment, SlackSession session, ReviewRequestService reviewRequestService, SubscriptionService subscriptionService,
        GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator)
    {
        this.targetChannel = fromChannel;
        this.fromChannel = fromChannel;
        this.changeId = changeId;
        this.comment = comment;
        this.session = session;
        this.reviewRequestService = reviewRequestService;
        this.subscriptionService = subscriptionService;
        this.gerritChangeInfoService = gerritChangeInfoService;
        this.changeInfoDecorator = changeInfoDecorator;
    }

    MessageHandler(SlackBot bot, SlackChannel fromChannel, String changeId, String comment, SlackSession session, ReviewRequestService reviewRequestService, SubscriptionService subscriptionService,
        GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator)
    {
        this.targetChannel = fromChannel;
        this.fromChannel = fromChannel;
        this.changeId = changeId;
        this.comment = comment;
        this.session = session;
        this.reviewRequestService = reviewRequestService;
        this.subscriptionService = subscriptionService;
        this.gerritChangeInfoService = gerritChangeInfoService;
        this.changeInfoDecorator = changeInfoDecorator;
    }

    MessageHandler(SlackUser sender, String targetChannelName, SlackChannel fromChannel, String changeId, String comment, SlackSession session, ReviewRequestService reviewRequestService,
                   SubscriptionService subscriptionService, GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator)
    {
        this.targetChannelName = targetChannelName;
        this.fromChannel = fromChannel;
        this.changeId = changeId;
        this.comment = comment;
        this.session = session;
        this.reviewRequestService = reviewRequestService;
        this.subscriptionService = subscriptionService;
        this.gerritChangeInfoService = gerritChangeInfoService;
        this.changeInfoDecorator = changeInfoDecorator;
    }

    MessageHandler(SlackBot bot, String targetChannelName, SlackChannel fromChannel, String changeId, String comment, SlackSession session, ReviewRequestService reviewRequestService, SubscriptionService subscriptionService,
        GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator)
    {
        this.targetChannelName = targetChannelName;
        this.fromChannel = fromChannel;
        this.changeId = changeId;
        this.comment = comment;
        this.session = session;
        this.reviewRequestService = reviewRequestService;
        this.subscriptionService = subscriptionService;
        this.gerritChangeInfoService = gerritChangeInfoService;
        this.changeInfoDecorator = changeInfoDecorator;
    }

    @Override
    public void run()
    {
        try
        {
            if (targetChannel == null)
            {
                targetChannel = session.findChannelByName(targetChannelName);
            }
            if (targetChannel == null)
            {
                session.sendMessage(fromChannel, "Unknown channel *`" + targetChannel + "`*", null, SlackChatConfiguration.getConfiguration().asUser());
            }
            else
            {
                ChangeInfo changeInfo = gerritChangeInfoService.getChangeInfo(changeId, true);
                if (changeInfo != null)
                {
                    SlackAttachment attachment = changeInfoDecorator.createAttachment(changeId, changeInfo, session);

                    Collection<String> channelsListeningToProject = subscriptionService.getChannelsListeningToProject(changeInfo.getProject());
                    if (!channelsListeningToProject.contains(targetChannel.getId()))
                    {
                        channelsListeningToProject = new ArrayList<String>(channelsListeningToProject);
                        channelsListeningToProject.add(targetChannel.getId());
                    }

                    Collection<String> channelsListeningToUser = subscriptionService.getChannelsListeningToUser(changeInfo.getOwner());
                    if (!channelsListeningToUser.contains(targetChannel.getId()))
                    {
                        channelsListeningToUser = new ArrayList<String>(channelsListeningToUser);
                        channelsListeningToUser.add(targetChannel.getId());
                    }

                    Collection<String> channelsListening = new ArrayList<>(channelsListeningToProject);
                    channelsListeningToUser.removeAll(channelsListeningToProject);
                    channelsListening.addAll(channelsListeningToUser);

                    for (String channelId : channelsListening)
                    {
                        SlackChannel channel = session.findChannelById(channelId);
                        SlackMessageHandle<SlackMessageReply> handle = session.sendMessage(channel, comment, attachment, SlackChatConfiguration.getConfiguration().asUser());
                        ReviewRequest previousRequest = reviewRequestService.getReviewRequest(channel.getId(), changeId);
                        ReviewRequest newRequest = new ReviewRequest(handle.getReply().getTimestamp(), changeId, channel.getId());
                        reviewRequestService.registerReviewRequest(newRequest);
                        if (previousRequest != null)
                        {
                            session.deleteMessage(previousRequest.getLastRequestTimestamp(), channel);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            session.sendMessage(fromChannel, "Could not find change id *`" + changeId + "`*, check that the change id is valid and does not correspond to a draft", null, SlackChatConfiguration.getConfiguration().asUser());
            e.printStackTrace();
        }
    }
}
