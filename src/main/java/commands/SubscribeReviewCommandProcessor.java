package commands;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.subscription.ProjectSubscriptionService;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackChatConfiguration;

@Singleton
public class SubscribeReviewCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private GerritChangeInfoService    gerritChangeInfoService;
    @Inject
    private ProjectSubscriptionService projectSubscriptionService;
    @Inject
    private ExecutorService            executor;

    private static Pattern             SUBSCRIBE_REVIEW_PATTERN = Pattern.compile("!subscribereview\\s(.*)");

    private class SubscriptionMessageHandler implements Runnable
    {

        SlackChannel channelToSubscribe;
        String       projectId;
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
                projectSubscriptionService.subscribeOnProject(projectId, channelToSubscribe.getId());
                session.sendMessage(channelToSubscribe, "This channel will now publish review requests from project *`" + projectId + "`*", null, SlackChatConfiguration.getConfiguration().asUser());
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
        Matcher matcher = SUBSCRIBE_REVIEW_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String projectId = matcher.group(1);
            SlackChannel channel = event.getChannel();
            executor.execute(new SubscriptionMessageHandler(channel, projectId, session));
            return true;
        }
        return false;
    }

}
