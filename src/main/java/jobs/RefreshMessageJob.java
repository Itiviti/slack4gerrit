package jobs;

import java.io.IOException;
import java.util.Collection;
import com.ullink.slack.review.gerrit.ChangeInfo;
import com.ullink.slack.review.gerrit.ChangeInfoFormatter;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequest;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.simpleslackapi.ChannelHistoryModule;
import com.ullink.slack.simpleslackapi.SlackAttachment;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackChatConfiguration;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.ChannelHistoryModuleFactory;

public class RefreshMessageJob implements Runnable
{
    private final String                           changeId;
    private final SlackSession               session;
    private final ReviewRequestService       reviewRequestService;
    private final GerritChangeInfoService gerritChangeInfoService;
    private final ChangeInfoFormatter changeInfoDecorator;

    public RefreshMessageJob(String changeId, SlackSession session, ReviewRequestService reviewRequestService, GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator)
    {
        this.changeId = changeId;
        this.session = session;
        this.reviewRequestService = reviewRequestService;
        this.gerritChangeInfoService = gerritChangeInfoService;
        this.changeInfoDecorator = changeInfoDecorator;
    }

    @Override
    public void run()
    {
        Collection<ReviewRequest> reviewRequests = reviewRequestService.getReviewRequests(changeId);
        ChannelHistoryModule channelHistoryModule = ChannelHistoryModuleFactory.createChannelHistoryModule(session);
        try
        {
            ChangeInfo changeInfo = gerritChangeInfoService.getChangeInfo(changeId);
            if (changeInfo != null)
            {
                SlackAttachment attachment = changeInfoDecorator.createAttachment(changeId, changeInfo, session);

                for (ReviewRequest reviewRequest : reviewRequests)
                {
                    SlackChannel channel = session.findChannelById(reviewRequest.getChannelId());
                    if (channel != null)
                    {
                        SlackMessagePosted existingMessage = channelHistoryModule.fetchMessageFromChannel(channel.getId(), reviewRequest.getLastRequestTimestamp());
                        String messageContent = "";
                        if (existingMessage != null && existingMessage.getMessageContent() != null) {
                            messageContent = existingMessage.getMessageContent();
                        }
                        session.updateMessage(reviewRequest.getLastRequestTimestamp(), channel, messageContent, new SlackAttachment[] {attachment});
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
