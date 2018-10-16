package commands;

import static commands.RegexConstants.CHANGE_ID;
import static commands.RegexConstants.COMMENT;
import static commands.RegexConstants.SPACES;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import jobs.PublishMessageJob;
import com.ullink.slack.review.gerrit.ChangeInfoFormatter;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.review.subscription.SubscriptionService;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

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
            String[] changeIds = matcher.group(1).split(SPACES);
            String comment = matcher.group(4);
            for (int i = 0; i < changeIds.length; i++)
            {
                executor.execute(new PublishMessageJob(event.getChannel(), changeIds[i].trim(), comment, session, reviewRequestService, subscriptionService, gerritChangeInfoService, changeInfoDecorator));
            }
            return true;
        }
        return false;
    }

}
