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
public class SubscribeAuthorCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private GerritChangeInfoService gerritChangeInfoService;
    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private ExecutorService executor;

    private static Pattern SUBSCRIBE_REVIEW_AUTHOR_PATTERN = Pattern.compile("!subscribereview\\s[^@](.*)");

    private class SubscriptionMessageHandler implements Runnable
    {

        SlackChannel channelToSubscribe;
        String userId;
        SlackSession session;

        public SubscriptionMessageHandler(SlackChannel channelToSubscribe, String userId, SlackSession session)
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
                    session.sendMessage(channelToSubscribe, "Could not find project name *`" + userId + "`*, check that this project name is valid and that it is active", null, SlackChatConfiguration.getConfiguration().asUser());
                    return;
                }
                subscriptionService.subscribeOnUser(userId, channelToSubscribe.getId());
                session.sendMessage(channelToSubscribe, "This channel will now publish review requests from user *`" + userId + "`*", null, SlackChatConfiguration.getConfiguration().asUser());
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
        Matcher matcher = SUBSCRIBE_REVIEW_AUTHOR_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String userId = matcher.group(1);
            SlackChannel channel = event.getChannel();
            executor.execute(new SubscriptionMessageHandler(channel, userId, session));
            return true;
        }
        return false;
    }
}
