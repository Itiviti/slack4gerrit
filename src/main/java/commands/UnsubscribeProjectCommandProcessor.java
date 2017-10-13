package commands;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.subscription.SubscriptionService;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.SlackChatConfiguration;

@Singleton
public class UnsubscribeProjectCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private ExecutorService executor;
    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private GerritChangeInfoService gerritChangeInfoService;

    private static Pattern UNSUBSCRIBE_REVIEW_PROJECT_PATTERN = Pattern.compile("!unsubscribereview\\s(.*)");

    private class UnsubscriptionMessageHandler implements Runnable
    {

        SlackChannel channelToSubscribe;
        String       projectId;
        SlackSession session;

        public UnsubscriptionMessageHandler(SlackChannel channelToSubscribe, String projectId, SlackSession session)
        {
            this.channelToSubscribe = channelToSubscribe;
            this.projectId = projectId;
            this.session = session;
        }

        @Override
        public void run()
        {
            try
            {
                if (!gerritChangeInfoService.projectExists(projectId))
                {
                    session.sendMessage(channelToSubscribe, "Could not find project name *`" + projectId + "`*, check that this project name is valid and that it is active", null, SlackChatConfiguration.getConfiguration().asUser());
                    return;
                }
                subscriptionService.unsubscribeOnProject(projectId, channelToSubscribe.getId());
                session.sendMessage(channelToSubscribe, "This channel will not publish any more review requests from project *`" + projectId + "`*", null, SlackChatConfiguration.getConfiguration().asUser());
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
        Matcher matcher = UNSUBSCRIBE_REVIEW_PROJECT_PATTERN.matcher(command);
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
