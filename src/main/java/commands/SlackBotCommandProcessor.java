package commands;

import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.model.event.MessageEvent;

public interface SlackBotCommandProcessor
{
    boolean process(String command, EventsApiPayload<MessageEvent> event, App app);

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
