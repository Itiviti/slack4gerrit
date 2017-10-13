package commands;

import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.subscription.SubscriptionService;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.SlackChatConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class UnsubscribeAuthorCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private ExecutorService executor;
    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private GerritChangeInfoService gerritChangeInfoService;

    private static Pattern UNSUBSCRIBE_REVIEW_AUTHOR_PATTERN = Pattern.compile("!unsubscribereview\\s[^@](.*)");

    private class UnsubscriptionMessageHandler implements Runnable
    {

        SlackChannel channelToSubscribe;
        String userId;
        SlackSession session;

        public UnsubscriptionMessageHandler(SlackChannel channelToSubscribe, String userId, SlackSession session)
        {
            this.channelToSubscribe = channelToSubscribe;
            this.userId = userId;
            this.session = session;
        }

        @Override
        public void run()
        {
            try
            {
                if (!gerritChangeInfoService.userExists(userId))
                {
                    session.sendMessage(channelToSubscribe, "Could not find a user named *`" + userId + "`*, check that the user name is valid and that it is active", null, SlackChatConfiguration.getConfiguration().asUser());
                    return;
                }
                subscriptionService.unsubscribeOnUser(userId, channelToSubscribe.getId());
                session.sendMessage(channelToSubscribe, "This channel will not publish any more review requests from user *`" + userId + "`*", null, SlackChatConfiguration.getConfiguration().asUser());
            }
            catch (IOException e)
            {
                session.sendMessage(channelToSubscribe, "Too bad, an unexpected error occurred...", null, SlackChatConfiguration.getConfiguration().asUser());
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean process(String command, SlackMessagePosted event, SlackSession session)
    {
        Matcher matcher = UNSUBSCRIBE_REVIEW_AUTHOR_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String projectId = matcher.group(1);
            SlackChannel channel = event.getChannel();
            executor.execute(new UnsubscriptionMessageHandler(channel, projectId, session));
            return true;
        }
        return false;
    }

}
