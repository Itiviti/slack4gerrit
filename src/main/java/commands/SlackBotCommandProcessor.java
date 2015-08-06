package commands;

import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public interface SlackBotCommandProcessor
{
    boolean process(String command,SlackMessagePosted event, SlackSession session);
}
