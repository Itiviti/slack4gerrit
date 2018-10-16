package com.ullink.slack.review;

import java.util.ArrayList;
import java.util.List;
import commands.HelpCommandProcessor;
import commands.ListReviewCommandProcessor;
import commands.PublishReviewCommandProcessor;
import commands.ReviewCommandProcessor;
import commands.SlackBotCommandProcessor;
import commands.SubscribeProjectCommandProcessor;
import commands.UnsubscribeProjectCommandProcessor;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;

public class ReviewMessageListener implements SlackMessagePostedListener
{

    private static final String ACK_EMOJI = "white_check_mark";
    List<SlackBotCommandProcessor> commandProcessors = new ArrayList<>();

    public ReviewMessageListener()
    {
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
    public void onEvent(SlackMessagePosted event, SlackSession session)
    {
        if (event.getMessageContent() != null)
        {
            String messageContent = event.getMessageContent();
            String lines[] = messageContent.split("\\r?\\n");
            int count = 0;
            for (String line : lines)
            {
                for (SlackBotCommandProcessor processor : commandProcessors)
                {
                    if (processor.process(line, event, session))
                    {
                        count++;
                    }
                }
            }
            if (count > 0)
            {
                session.addReactionToMessage(event.getChannel(), event.getTimeStamp(), ACK_EMOJI);
            }
        }
    }
}
