package jobs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatDeleteRequest;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.Attachment;
import com.ullink.slack.review.gerrit.ChangeInfo;
import com.ullink.slack.review.gerrit.ChangeInfoFormatter;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequest;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.review.subscription.SubscriptionService;

public class PublishMessageJob implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishMessageJob.class);

    private String fromChannelId;
    private String targetChannelId;
    private String                           targetChannelName;
    private String                           changeId;
    private String                           comment = "";
    private final App app;
    private final ReviewRequestService       reviewRequestService;
    private final SubscriptionService subscriptionService;
    private final GerritChangeInfoService    gerritChangeInfoService;
    private final ChangeInfoFormatter        changeInfoDecorator;

    public PublishMessageJob(String fromChannelId, String changeId, String comment, App app, ReviewRequestService reviewRequestService, SubscriptionService subscriptionService,
        GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator)
    {
        this.targetChannelId = fromChannelId;
        this.fromChannelId = fromChannelId;
        this.changeId = changeId;
        this.comment = comment;
        this.app = app;
        this.reviewRequestService = reviewRequestService;
        this.subscriptionService = subscriptionService;
        this.gerritChangeInfoService = gerritChangeInfoService;
        this.changeInfoDecorator = changeInfoDecorator;
    }

    public PublishMessageJob(String targetChannelName, String fromChannelId, String changeId, String comment, App app, ReviewRequestService reviewRequestService,
                   SubscriptionService subscriptionService, GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator)
    {
        this.targetChannelName = targetChannelName;
        this.fromChannelId = fromChannelId;
        this.changeId = changeId;
        this.comment = comment;
        this.app = app;
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
            if (targetChannelId == null)
            {
                //TODO: find a way to retrieve channel by name
                //targetChannelId = app.findChannelByName(targetChannelName);
            }
            if (targetChannelId == null)
            {
                try
                {
                    app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(fromChannelId).text("Unknown channel *`" + targetChannelId + "`*").build());
                }
                catch (SlackApiException e)
                {
                    //throw new RuntimeException(e);
                    //TODO log
                }
            }
            else
            {
                ChangeInfo changeInfo = gerritChangeInfoService.getChangeInfo(changeId);
                if (changeInfo != null)
                {
                    Attachment attachment = changeInfoDecorator.createAttachment(changeId, changeInfo, app);

                    Collection<String> channelsListeningToProject = subscriptionService.getChannelsListeningToProject(changeInfo.getProject());
                    if (!channelsListeningToProject.contains(targetChannelId))
                    {
                        channelsListeningToProject = new ArrayList<>(channelsListeningToProject);
                        channelsListeningToProject.add(targetChannelId);
                    }

                    Collection<String> channelsListeningToUser = subscriptionService.getChannelsListeningToUser(changeInfo.getOwner());
                    if (!channelsListeningToUser.contains(targetChannelId))
                    {
                        channelsListeningToUser = new ArrayList<>(channelsListeningToUser);
                        channelsListeningToUser.add(targetChannelId);
                    }

                    Collection<String> channelsListening = new ArrayList<>(channelsListeningToProject);
                    channelsListeningToUser.removeAll(channelsListeningToProject);
                    channelsListening.addAll(channelsListeningToUser);

                    for (String channelId : channelsListening)
                    {
                        try
                        {
                            List<Attachment> attachmentList = new ArrayList<>();
                            attachmentList.add(attachment);
                            ChatPostMessageResponse response = app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(channelId).text(comment).attachments(attachmentList).build());
                            ReviewRequest previousRequest = reviewRequestService.getReviewRequest(channelId, changeId);
                            ReviewRequest newRequest = new ReviewRequest(response.getTs(), changeId, channelId);
                            reviewRequestService.registerReviewRequest(newRequest);
                            if (previousRequest != null)
                            {
                                app.getClient().chatDelete(ChatDeleteRequest.builder().channel(channelId).ts(previousRequest.getLastRequestTimestamp()).build());
                            }
                        }
                        catch (SlackApiException e)
                        {
                            LOGGER.error("Slack API error: ", e);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            try
            {
                app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(fromChannelId).text("Could not find change id *`" + changeId + "`*, check that the change id is valid and does not correspond to a draft").build());
            }
            catch (IOException | SlackApiException ex)
            {
                LOGGER.error("Slack API error: ", ex);
            }
            LOGGER.error("Could not publish review for change id " + changeId, e);
        }
    }
}
