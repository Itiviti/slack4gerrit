package commands;

import static commands.RegexConstants.ANYTHING_ELSE;
import static commands.RegexConstants.SPACES;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.event.MessageEvent;

public class HelpCommandProcessor implements SlackBotCommandProcessor
{
    private static final String COMMAND = "!help";
    private static Pattern HELP_PATTERN = Pattern.compile(COMMAND + "(" + SPACES + "(" + ANYTHING_ELSE + "))?");
    private Collection<SlackBotCommandProcessor> commands = Collections.emptyList();

    @Override
    public boolean process(String command, EventsApiPayload<MessageEvent> event, App app)
    {
        Matcher matcher = HELP_PATTERN.matcher(command);
        if (!matcher.matches())
        {
            return false;
        }
        String topic = matcher.groupCount() >= 2 ? matcher.group(2) : null;

        if (topic == null)
        {
            sendHelp(event, app, "Here is all I can do for you:"
                + lineSeparator()
                + help(it -> true)
            );
        }
        else if (commands.stream().map(SlackBotCommandProcessor::name).anyMatch(topic::equals))
        {
            sendHelp(event, app, "Here is what I can do for you on " + topic + ":"
                + lineSeparator()
                + help(it -> it.name().equals(topic)));
        }
        else
        {
            sendHelp(event, app, "I cannot do anything on " + topic
                + lineSeparator()
                + help());
        }

        return true;
    }


    private String help(Predicate<SlackBotCommandProcessor> matchingPredicate)
    {
        return commands.stream()
            .filter(matchingPredicate)
            .flatMap(it ->
                Stream.of(
                    "`" + it.name() + "`",
                    "*Pattern*: " + it.pattern(),
                    "*Behavior*: " + it.help()))
            .collect(joining(lineSeparator()));
    }

    private void sendHelp(EventsApiPayload<MessageEvent> event, App app, String message)
    {
        try
        {
            app.getClient().chatPostMessage(ChatPostMessageRequest.builder().channel(event.getEvent().getChannel()).text(message).build());
        }
        catch (IOException | SlackApiException e)
        {
            //throw new RuntimeException(e);
            //TODO log
        }
    }

    @Override
    public String name()
    {
        return COMMAND;
    }

    @Override
    public String help()
    {
        return "explains the different actions I can do for you";
    }

    public void setCommands(List<SlackBotCommandProcessor> commandProcessors)
    {
        commands = new ArrayList<>(commandProcessors);
    }
}
