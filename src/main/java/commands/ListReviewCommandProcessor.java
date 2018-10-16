package commands;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.ullink.slack.review.subscription.SubscriptionService;
import com.ullink.slack.simpleslackapi.SlackChatConfiguration;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

@Singleton
public class ListReviewCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private SubscriptionService subscriptionService;

    private static final String COMMAND = "!listreviewsubscription";

    private static Pattern LIST_REVIEW_PATTERN = Pattern.compile(COMMAND);

    @Override
    public boolean process(String command, SlackMessagePosted event, SlackSession session)
    {
        Matcher matcher = LIST_REVIEW_PATTERN.matcher(command);
        if (matcher.matches())
        {
            Collection<String> projects = subscriptionService.getChannelSubscriptions(event.getChannel().getId());
            session.sendMessage(event.getChannel(), "This channel is listening to *`" + projects + "`*", null, SlackChatConfiguration.getConfiguration().asUser());
            return true;
        }
        return false;
    }

}
