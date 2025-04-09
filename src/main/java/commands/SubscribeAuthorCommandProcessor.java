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
public class SubscribeAuthorCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private GerritChangeInfoService gerritChangeInfoService;
    @Inject
    private SubscriptionService subscriptionService;
    @Inject
    private ExecutorService executor;

    private static final String COMMAND = "!subscribereview";

    private static Pattern SUBSCRIBE_REVIEW_AUTHOR_PATTERN = Pattern.compile(COMMAND + SPACES + "(" + USER_ALIAS + ")" + ANYTHING_ELSE);

    @Override
    public boolean process(String command, EventsApiPayload<MessageEvent> event, App app)
    {
        Matcher matcher = SUBSCRIBE_REVIEW_AUTHOR_PATTERN.matcher(command);
        if (matcher.matches())
        {
            String userId = matcher.group(1);
            executor.execute(new SubscriptionMessageHandler(event.getEvent().getChannel(), userId, app));
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
        return "will subscribe the current channel to review requests from <user>";
    }

    private class SubscriptionMessageHandler implements Runnable
    {
        String channelIdToSubscribe;
        String userId;
        App app;

        public SubscriptionMessageHandler(String channelIdToSubscribe, String userId, App app)
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
                    app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(channelIdToSubscribe).text("Could not find user name *`" + userId + "`*, check that this user exists, is valid and active").build());
                    return;
                }
                subscriptionService.subscribeOnUser(userId, channelIdToSubscribe);
                app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(channelIdToSubscribe).text("This channel will now publish review requests from user *`" + userId + "`*").build());
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
