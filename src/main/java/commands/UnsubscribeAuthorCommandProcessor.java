package commands;

import static commands.RegexConstants.ANYTHING_ELSE;
import static commands.RegexConstants.SPACES;
import static commands.RegexConstants.USER_ALIAS;

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
public class UnsubscribeAuthorCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private ExecutorService executor;
    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private GerritChangeInfoService gerritChangeInfoService;

    private static final String COMMAND = "!unsubscribereview";
    private static Pattern UNSUBSCRIBE_REVIEW_AUTHOR_PATTERN = Pattern.compile(COMMAND + SPACES + "(" + USER_ALIAS + ")" + ANYTHING_ELSE);

    @Override
    public boolean process(String command, EventsApiPayload<MessageEvent> event, App app)
    {
        Matcher matcher = UNSUBSCRIBE_REVIEW_AUTHOR_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String projectId = matcher.group(1);
            String channelId = event.getEvent().getChannel();
            executor.execute(new UnsubscriptionMessageHandler(channelId, projectId, app));
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
        return COMMAND + " @<user>";
    }

    @Override
    public String help()
    {
        return "will unsubscribe the current channel to review requests from <user>";
    }

    private class UnsubscriptionMessageHandler implements Runnable
    {

        String channelIdToSubscribe;
        String userId;
        App app;

        public UnsubscriptionMessageHandler(String channelIdToSubscribe, String userId, App app)
        {
            this.channelIdToSubscribe = channelIdToSubscribe;
            this.userId = userId;
            this.app = app;
        }

        @Override
        public void run()
        {
            try
            {
                if (!gerritChangeInfoService.userExists(userId))
                {
                    app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(channelIdToSubscribe).text("Could not find a user named *`" + userId + "`*, check that the user name is valid and that it is active").build());
                    return;
                }
                subscriptionService.unsubscribeOnUser(userId, channelIdToSubscribe);
                app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(channelIdToSubscribe).text("This channel will not publish any more review requests from user *`" + userId + "`*").build());
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
