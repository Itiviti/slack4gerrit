package commands;

import static commands.RegexConstants.CHANGE_ID;
import static commands.RegexConstants.CHANNEL;
import static commands.RegexConstants.COMMENT;
import static commands.RegexConstants.SPACES;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import jobs.PublishMessageJob;
import com.ullink.slack.review.gerrit.ChangeInfoFormatter;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.review.subscription.SubscriptionService;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

@Singleton
public class PublishReviewCommandProcessor implements SlackBotCommandProcessor
{
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
    public boolean process(String command, SlackMessagePosted event, SlackSession session)
    {
        Matcher matcher = PUBLISH_REVIEW_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String channelNameToPublish = matcher.group(1);
            String changeId = matcher.group(2);
            String comment = matcher.group(4);
            SlackChannel channel = session.findChannelByName(channelNameToPublish);
            if (channel != null)
            {
                executor.execute(new PublishMessageJob(channelNameToPublish, event.getChannel(), changeId.trim(), comment, session, reviewRequestService, subscriptionService, gerritChangeInfoService, changeInfoDecorator));
            }
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
