/*************************************************************************
 * ULLINK CONFIDENTIAL INFORMATION
 * _______________________________
 *
 * All Rights Reserved.
 *
 * NOTICE: This file and its content are the property of Ullink. The
 * information included has been classified as Confidential and may
 * not be copied, modified, distributed, or otherwise disseminated, in
 * whole or part, without the express written permission of Ullink.
 *************************************************************************/
package commands;

import static commands.RegexConstants.ANYTHING_ELSE;
import static commands.RegexConstants.SPACES;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.ullink.slack.simpleslackapi.SlackChatConfiguration;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

public class HelpCommandProcessor implements SlackBotCommandProcessor
{
    private static final String COMMAND = "!help";
    private static Pattern LIST_REVIEW_PATTERN = Pattern.compile(COMMAND + "(" + SPACES + "(" + ANYTHING_ELSE + "))?");
    private Collection<SlackBotCommandProcessor> commands = Collections.emptyList();

    @Override
    public boolean process(String command, SlackMessagePosted event, SlackSession session)
    {
        Matcher matcher = LIST_REVIEW_PATTERN.matcher(command);
        if (!matcher.matches())
        {
            return false;
        }
        String topic = matcher.groupCount() >= 2 ? matcher.group(2) : null;

        if (topic == null)
        {
            sendHelp(event, session, "Here is all I can do for you:"
                + lineSeparator()
                + help(it -> true)
            );
        }
        else if (commands.stream().map(SlackBotCommandProcessor::name).anyMatch(topic::equals))
        {
            sendHelp(event, session, "Here is what I can do for you on " + topic + ":"
                + lineSeparator()
                + help(it -> it.name().equals(topic)));
        }
        else
        {
            sendHelp(event, session, "I cannot do anything on " + topic
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

    private void sendHelp(SlackMessagePosted event, SlackSession session, String message)
    {
        session.sendMessage(event.getChannel(), message, null, SlackChatConfiguration.getConfiguration().asUser());
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
