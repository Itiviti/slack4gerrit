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
package commands

import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Unroll
class HelpSpec extends Specification {

    @Shared
    def processor = new SlackBotCommandProcessor() {
        @Override
        boolean process(String command, SlackMessagePosted event, SlackSession session) { false }

        @Override
        String name() { 'command' }

        @Override
        String pattern() { 'command pattern' }

        @Override
        String help() { 'I can help' }
    }

    @Subject
    @Shared
    def help = new HelpCommandProcessor()

    def "with #availableCommands, '#command' answer will contain '#expected' and not '#unexpected'"() {
        def actualArgs
        given:
        def channel = Mock(SlackChannel)//new SlackChannel('channel', 'name', 'topic', 'purpose', false, true, false)

        def session = Mock(SlackSession)
        def event = Mock(SlackMessagePosted) {
            _ * it.getChannel() >> channel
        }

        help.setCommands(availableCommands)

        when:
        help.process(command, event, session)
        then:
        _ * session.sendMessage(*_) >> { args ->
            actualArgs = args
            return null
        }
        def message = actualArgs[1] as String
        actualArgs[0] == channel
        message.contains(expected)
        !message.contains(unexpected)

        where:
        availableCommands | command                     | expected            | unexpected
        [processor]       | '!help ' + processor.name() | processor.help()    | help.help()
        [processor]       | '!help ' + processor.name() | processor.name()    | help.help()
        [processor]       | '!help ' + processor.name() | processor.pattern() | help.pattern()
        [help]            | '!help ' + help.name()      | help.help()         | processor.help()
        [help]            | '!help '                    | help.help()         | processor.help()
    }

}
