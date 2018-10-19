package commands;

import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public interface SlackBotCommandProcessor
{
    boolean process(String command, SlackMessagePosted event, SlackSession session);

    /**
     * @return the name of the command
     */
    default String name()
    {
        return getClass().getSimpleName();
    }

    /**
     * @return the Pattern used to match the command
     */
    default String pattern()
    {
        return name();
    }

    /**
     * @return the help message for the command
     */
    default String help()
    {
        return "no help available";
    }
}
