package commands;

import java.io.IOException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.event.MessageEvent;
import com.ullink.slack.review.subscription.SubscriptionService;

@Singleton
public class ListReviewCommandProcessor implements SlackBotCommandProcessor
{
    @Inject
    private SubscriptionService subscriptionService;

    private static final String COMMAND = "!listreviewsubscription";

    private static Pattern LIST_REVIEW_PATTERN = Pattern.compile(COMMAND);

    @Override
    public boolean process(String command, EventsApiPayload<MessageEvent> event, App app)
    {
        Matcher matcher = LIST_REVIEW_PATTERN.matcher(command);
        if (matcher.matches())
        {
            Collection<String> projects = subscriptionService.getChannelSubscriptions(event.getEvent().getChannel());
            try
            {
                app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(event.getEvent().getChannel()).text("This channel is listening to *`" + projects + "`*").build());
            }
            catch (IOException | SlackApiException e)
            {
                //throw new RuntimeException(e);
                //TODO handle exception
            }
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
    public String help()
    {
        return "will list the channel subscriptions";
    }
}
