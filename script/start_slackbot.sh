#!/bin/sh
export SLACK_BOT_TOKEN=<your bot token given by Slack>
export SLACK_APP_TOKEN=<your app token given by Slack>
java -classpath "./lib/*" com.ullink.slack.review.Connector 2> log.txt