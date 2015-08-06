package commands;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.ullink.slack.review.subscription.ProjectSubscriptionService;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackChatConfiguration;

@Singleton
public class ListReviewCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private ProjectSubscriptionService projectSubscriptionService;

    private static Pattern             LIST_REVIEW_PATTERN = Pattern.compile("!listreviewsubscription");

    @Override
    public boolean process(String command, SlackMessagePosted event, SlackSession session)
    {
        Matcher matcher = LIST_REVIEW_PATTERN.matcher(command);
        int count = 0;
        if (matcher.matches())
        {
            Collection<String> projects = projectSubscriptionService.getChannelSubscriptions(event.getChannel().getId());
            session.sendMessage(event.getChannel(), "This channel is listening to *`" + projects + "`*", null, SlackChatConfiguration.getConfiguration().asUser());
            return true;
        }
        return false;
    }

}
