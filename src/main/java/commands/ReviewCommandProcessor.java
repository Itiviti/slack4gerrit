package commands;

import static commands.RegexConstants.CHANGE_ID;
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
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ReviewCommandProcessor implements SlackBotCommandProcessor
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

    private static final Logger LOGGER       = LoggerFactory.getLogger(ReviewCommandProcessor.class);

    private static final String COMMAND = "!review";
    private static Pattern REVIEW_PATTERN = Pattern.compile(COMMAND
        + "((" + SPACES + CHANGE_ID + ")+)"
        + "(" + SPACES + "(" + COMMENT + "))?");

    @Override
    public boolean process(String command, SlackMessagePosted event, SlackSession session)
    {
        Matcher matcher = REVIEW_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String[] changeIds = matcher.group(1).trim().split(SPACES);
            String comment = matcher.group(4);
            for (int i = 0; i < changeIds.length; i++)
            {
                if (changeIds[i] != null && !changeIds[i].trim().isEmpty())
                {
                    executor.execute(new PublishMessageJob(event.getChannel(), changeIds[i].trim(), comment, session, reviewRequestService, subscriptionService, gerritChangeInfoService, changeInfoDecorator));
                }
                else
                {
                    LOGGER.error("Incorrect changeId '" + changeIds[i] + "' for command" + command);
                }
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
        return
            name() + " <changeId>( <changeId>)* <comment>`";
    }

    @Override
    public String help()
    {
        return Stream.of(
            "will publish the details of multiple changes to review to this channel.",
            "<changeId>: the change(s) to publish",
            "<comment>: a comment that will be published with the change"
        ).collect(joining(lineSeparator()));
    }

}
