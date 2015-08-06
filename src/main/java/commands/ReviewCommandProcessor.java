package commands;

import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.ullink.slack.review.gerrit.ChangeInfoFormatter;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.review.subscription.ProjectSubscriptionService;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

@Singleton
public class ReviewCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private ExecutorService            executor;
    @Inject
    private ReviewRequestService       reviewRequestService;
    @Inject
    private ProjectSubscriptionService projectSubscriptionService;
    @Inject
    private GerritChangeInfoService    gerritChangeInfoService;
    @Inject
    private ChangeInfoFormatter        changeInfoDecorator;

    private static Pattern             REVIEW_PATTERN = Pattern.compile("!review\\s+(((\\d+)\\s*)+)\\s*(.*)");

    @Override
    public boolean process(String command, SlackMessagePosted event, SlackSession session)
    {
        Matcher matcher = REVIEW_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String[] changeIds = matcher.group(1).split(" ");
            String comment = matcher.group(4);
            for (int i = 0; i < changeIds.length; i++)
            {
                if (event.getSender() != null)
                {
                    executor.execute(new MessageHandler(event.getChannel(), changeIds[i].trim(), comment, session, reviewRequestService, projectSubscriptionService, gerritChangeInfoService, changeInfoDecorator));
                }
                else if (event.getBot() != null)
                {
                    executor.execute(new MessageHandler(event.getBot(), event.getChannel(), changeIds[i].trim(), comment, session, reviewRequestService, projectSubscriptionService, gerritChangeInfoService, changeInfoDecorator));
                }
            }
            return true;
        }
        return false;
    }

}
