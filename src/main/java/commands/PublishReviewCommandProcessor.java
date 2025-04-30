package commands;

import static commands.RegexConstants.CHANGE_ID;
import static commands.RegexConstants.CHANNEL;
import static commands.RegexConstants.COMMENT;
import static commands.RegexConstants.SPACES;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import com.ullink.slack.review.HttpHelper;
import jobs.PublishMessageJob;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.model.event.MessageEvent;
import com.ullink.slack.review.gerrit.ChangeInfoFormatter;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.review.subscription.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PublishReviewCommandProcessor implements SlackBotCommandProcessor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishReviewCommandProcessor.class);

    @Inject
    private ExecutorService executor;
    @Inject
    private ReviewRequestService reviewRequestService;
    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private GerritChangeInfoService gerritChangeInfoService;
    @Inject
    private ChangeInfoFormatter changeInfoDecorator;

    private static final String COMMAND = "!publishreview";
    private final Pattern PUBLISH_REVIEW_PATTERN = Pattern.compile(COMMAND + SPACES
        + "(" + CHANNEL + ")" + SPACES
        + "(" + CHANGE_ID + ")"
        + "(" + SPACES + "(" + COMMENT + "))?");

    public PublishReviewCommandProcessor()
    {
    }

    @Override
    public boolean process(String command, EventsApiPayload<MessageEvent> event, App app)
    {
        Matcher matcher = PUBLISH_REVIEW_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String channelNameToPublish = matcher.group(1);
            String changeId = matcher.group(2);
            String comment = matcher.group(4);

            executor.execute(new PublishMessageJob(channelNameToPublish, event.getEvent().getChannel(), changeId.trim(), comment, app, reviewRequestService, subscriptionService, gerritChangeInfoService, changeInfoDecorator));
            return true;
        }
        return false;
    }

    @Override
    public String name()
    {
        return COMMAND;
    }

    @Override
    public String pattern()
    {
        return COMMAND + " <channel> <changeId> <comment>";
    }

    @Override
    public String help()
    {
        return Stream.of(
            "will publish the details to review to a different channel.",
            "<channel>: channel to publish to",
            "<changeId>: the change to publish",
            "<comment>: a comment that will be published with the change"
        ).collect(joining(lineSeparator()));
    }

}
