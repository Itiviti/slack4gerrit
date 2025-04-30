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

import com.slack.api.app_backend.events.payload.EventsApiPayload
import com.slack.api.bolt.App
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.model.event.MessageEvent
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Unroll
class HelpSpec extends Specification {

    @Shared
    def processor = new SlackBotCommandProcessor() {

        @Override
        boolean process(String command, EventsApiPayload<MessageEvent> event, App app) {
            return false
        }

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
        def channelName = 'channel'
        def event = Mock(EventsApiPayload)
        def messageEvent = Mock(MessageEvent)
        messageEvent.getChannel() >> channelName
        event.getEvent() >> messageEvent
        def session = Mock(App)
        session.getClient() >> Mock(MethodsClient)

        help.setCommands(availableCommands)

        when:
        help.process(command, event, session)
        then:
        _ * session.getClient().chatPostMessage(*_) >> { args ->
            actualArgs = args
            return null
        }
        def messageReq = actualArgs[0] as ChatPostMessageRequest
        messageReq.channel == channelName
        messageReq.text.contains(expected)
        !messageReq.text.contains(unexpected)

        where:
        availableCommands | command                     | expected            | unexpected
        [processor]       | '!help ' + processor.name() | processor.help()    | help.help()
        [processor]       | '!help ' + processor.name() | processor.name()    | help.help()
        [processor]       | '!help ' + processor.name() | processor.pattern() | help.pattern()
        [help]            | '!help ' + help.name()      | help.help()         | processor.help()
        [help]            | '!help '                    | help.help()         | processor.help()
    }

}
