package commands;

import static commands.RegexConstants.ANYTHING_ELSE;
import static commands.RegexConstants.PROJECT;
import static commands.RegexConstants.SPACES;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.subscription.SubscriptionService;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackChatConfiguration;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

@Singleton
public class SubscribeProjectCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private GerritChangeInfoService gerritChangeInfoService;
    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private ExecutorService executor;

    private static final String COMMAND = "!subscribereview";
    private static Pattern SUBSCRIBE_REVIEW_PROJECT_PATTERN = Pattern.compile(COMMAND + SPACES + "(" + PROJECT + ")" + ANYTHING_ELSE);

    @Override
    public boolean process(String command, SlackMessagePosted event, SlackSession session)
    {
        Matcher matcher = SUBSCRIBE_REVIEW_PROJECT_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String projectId = matcher.group(1);
            SlackChannel channel = event.getChannel();
            executor.execute(new SubscriptionMessageHandler(channel, projectId, session));
            return true;
        }
        return false;
    }

    private class SubscriptionMessageHandler implements Runnable
    {

        SlackChannel channelToSubscribe;
        String projectId;
        SlackSession session;

        public SubscriptionMessageHandler(SlackChannel channelToSubscribe, String projectId, SlackSession session)
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
                subscriptionService.subscribeOnProject(projectId, channelToSubscribe.getId());
                session.sendMessage(channelToSubscribe, "This channel will now publish review requests from project *`" + projectId + "`*", null, SlackChatConfiguration.getConfiguration().asUser());
            }
            catch (IOException e)
            {
                session.sendMessage(channelToSubscribe, "Too bad, an unexpected error occurred...", null, SlackChatConfiguration.getConfiguration().asUser());
                e.printStackTrace();
            }
        }
    }
}

