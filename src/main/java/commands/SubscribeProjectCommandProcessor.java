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
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.event.MessageEvent;
import com.ullink.slack.review.gerrit.GerritChangeInfoService;
import com.ullink.slack.review.subscription.SubscriptionService;

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
    public boolean process(String command, EventsApiPayload<MessageEvent> event, App app)
    {
        Matcher matcher = SUBSCRIBE_REVIEW_PROJECT_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String projectId = matcher.group(1);
            String channelId = event.getEvent().getChannel();
            executor.execute(new SubscriptionMessageHandler(channelId, projectId, app));
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
        return COMMAND + " <project>";
    }

    @Override
    public String help()
    {
        return "will subscribe the current channel to review requests on <project>";
    }

    private class SubscriptionMessageHandler implements Runnable
    {
        String channelIdToSubscribe;
        String projectId;
        App app;

        public SubscriptionMessageHandler(String channelIdToSubscribe, String projectId, App app)
        {
            this.channelIdToSubscribe = channelIdToSubscribe;
            this.projectId = projectId;
            this.app = app;
        }

        @Override
        public void run()
        {
            try
            {
                if (!gerritChangeInfoService.projectExists(projectId))
                {
                    app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(channelIdToSubscribe).text("Could not find project name *`" + projectId + "`*, check that this project name is valid and that it is active").build());
                    return;
                }
                subscriptionService.subscribeOnProject(projectId, channelIdToSubscribe);
                app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(channelIdToSubscribe).text("This channel will now publish review requests from project *`" + projectId + "`*").build());
            }
            catch (IOException e)
            {
                //session.sendMessage(channelIdToSubscribe, "Too bad, an unexpected error occurred...", null, SlackChatConfiguration.getConfiguration().asUser());
                e.printStackTrace();
            }
            catch (SlackApiException e)
            {
                //throw new RuntimeException(e);
                //TODO log
            }
        }
    }
}

