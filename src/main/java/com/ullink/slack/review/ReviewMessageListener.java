package com.ullink.slack.review;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import commands.HelpCommandProcessor;
import commands.ListReviewCommandProcessor;
import commands.PublishReviewCommandProcessor;
import commands.ReviewCommandProcessor;
import commands.SlackBotCommandProcessor;
import commands.SubscribeProjectCommandProcessor;
import commands.UnsubscribeProjectCommandProcessor;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.reactions.ReactionsAddRequest;
import com.slack.api.model.event.MessageEvent;

public class ReviewMessageListener implements BoltEventHandler<MessageEvent>
{

    private static final String ACK_EMOJI = "white_check_mark";
    List<SlackBotCommandProcessor> commandProcessors = new ArrayList<>();
    private final App app;

    public ReviewMessageListener(App app)
    {
        this.app = app;
        HelpCommandProcessor helpCommandProcessor = Connector.injector.getInstance(HelpCommandProcessor.class);
        commandProcessors.add(Connector.injector.getInstance(ReviewCommandProcessor.class));
        commandProcessors.add(Connector.injector.getInstance(PublishReviewCommandProcessor.class));
        commandProcessors.add(Connector.injector.getInstance(SubscribeProjectCommandProcessor.class));
        commandProcessors.add(Connector.injector.getInstance(UnsubscribeProjectCommandProcessor.class));
        commandProcessors.add(Connector.injector.getInstance(ListReviewCommandProcessor.class));
        commandProcessors.add(helpCommandProcessor);
        helpCommandProcessor.setCommands(commandProcessors);
    }

    @Override
    public Response apply(EventsApiPayload<MessageEvent> event, EventContext context) throws IOException, SlackApiException
    {
        MessageEvent messageEvent = event.getEvent();
        String text = messageEvent.getText();
        if (text != null)
        {
            String lines[] = text.split("\\r?\\n");
            int count = 0;
            for (String line : lines)
            {
                for (SlackBotCommandProcessor processor : commandProcessors)
                {
                    if (processor.process(line, event, app))
                    {
                        count++;
                    }
                }
            }
            if (count > 0)
            {
                ReactionsAddRequest reaction = ReactionsAddRequest.builder().channel(messageEvent.getChannel()).timestamp(messageEvent.getTs()).name(ACK_EMOJI).build();
                app.getClient().reactionsAdd(reaction);
            }
        }
        return context.ack();
    }
}
