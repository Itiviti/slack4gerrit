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

import com.ullink.slack.review.gerrit.ChangeInfoFormatter
import com.ullink.slack.review.gerrit.GerritChangeInfoService
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService
import com.ullink.slack.review.subscription.SubscriptionService
import com.ullink.slack.simpleslackapi.SlackSession
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.util.concurrent.ExecutorService

@Unroll
class CommandProcessorsSpec extends Specification {

    def executor = Mock(ExecutorService)
    def subscriptionService = Mock(SubscriptionService)
    def gerritChangeInfoService = Mock(GerritChangeInfoService)
    def reviewRequestService = Mock(ReviewRequestService)
    def changeInfoDecorator = Mock(ChangeInfoFormatter)

    @Subject
    def commandProcessors = [
            new HelpCommandProcessor(), new ListReviewCommandProcessor(subscriptionService: subscriptionService),
            new PublishReviewCommandProcessor(executor: executor, reviewRequestService: reviewRequestService, subscriptionService: subscriptionService, gerritChangeInfoService: gerritChangeInfoService, changeInfoDecorator: changeInfoDecorator),
            new ReviewCommandProcessor(executor: executor, reviewRequestService: reviewRequestService, subscriptionService: subscriptionService, gerritChangeInfoService: gerritChangeInfoService, changeInfoDecorator: changeInfoDecorator),
            new SubscribeAuthorCommandProcessor(executor: executor, subscriptionService: subscriptionService, gerritChangeInfoService: gerritChangeInfoService),
            new SubscribeProjectCommandProcessor(executor: executor, subscriptionService: subscriptionService, gerritChangeInfoService: gerritChangeInfoService),
            new UnsubscribeAuthorCommandProcessor(executor: executor, subscriptionService: subscriptionService, gerritChangeInfoService: gerritChangeInfoService),
            new UnsubscribeProjectCommandProcessor(executor: executor, subscriptionService: subscriptionService, gerritChangeInfoService: gerritChangeInfoService)
    ]


    def "command '#command' should be processed by #expectedProcessors"() {
        given:
        def event = Mock(SlackMessagePosted)
        def session = Mock(SlackSession)

        when:
        def processors = commandProcessors.findAll { it.process(command, event, session) }.collect { it.class }

        then:
        processors as Set == expectedProcessors as Set

        where:
        command                        | expectedProcessors
        ''                             | []
        'not a command'                | []
        '!help'                        | [HelpCommandProcessor]
        '!help topic'                  | [HelpCommandProcessor]
        '!review'                      | []
        '!review comment'              | []
        '!review 12345 comment'        | [ReviewCommandProcessor]
        '!review 12345 123456'         | [ReviewCommandProcessor]
        '!review 12345 123456 comment' | [ReviewCommandProcessor]
        '!review 12345 123456 comment' | [ReviewCommandProcessor]
        '!subscribereview'             | []
        '!subscribereview something'   | [SubscribeProjectCommandProcessor]
        '!subscribereview @user'       | [SubscribeAuthorCommandProcessor]
        '!unsubscribereview'           | []
        '!unsubscribereview something' | [UnsubscribeProjectCommandProcessor]
        '!unsubscribereview @user'     | [UnsubscribeAuthorCommandProcessor]
    }
}
