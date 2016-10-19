package com.ullink.slack.review;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.ullink.slack.review.gerrit.ChangeInfoFormatter;
import com.ullink.slack.review.gerrit.DefaultChangeInfoFormatter;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestServiceImpl;
import com.ullink.slack.review.subscription.SubscriptionImpl;
import com.ullink.slack.review.subscription.SubscriptionService;

public class BaseModule extends AbstractModule
{
    private final Logger log = LoggerFactory.getLogger(BaseModule.class);

    private final Properties    properties;

    public BaseModule(Properties properties)
    {
        this.properties = properties;
    }

    @Override
    protected void configure()
    {
        ChangeInfoFormatter changeFormatter;
        try
        {
            changeFormatter = (ChangeInfoFormatter) Class.forName(properties.getProperty(Constants.CHANGE_INFO_FORMATTER_CLASS)).getDeclaredConstructor(Properties.class).newInstance(properties);
        }
        catch (Exception e)
        {
            log.warn("unable to load " + properties.getProperty(Constants.CHANGE_INFO_FORMATTER_CLASS) + " class as a change formatter, using the default one",e);
            changeFormatter = new DefaultChangeInfoFormatter(properties);
        }
        DB db = DBMaker.newFileDB(new File("db")).closeOnJvmShutdown().make();
        db.compact();
        bind(DB.class).toInstance(db);
        bind(String.class).annotatedWith(Names.named(Constants.GERRIT_URL)).toInstance(properties.getProperty(Constants.GERRIT_URL));
        bind(String.class).annotatedWith(Names.named(Constants.JIRA_URL)).toInstance(properties.getProperty(Constants.JIRA_URL));
        bind(String.class).annotatedWith(Names.named(Constants.JIRA_USER)).toInstance(properties.getProperty(Constants.JIRA_USER));
        bind(String.class).annotatedWith(Names.named(Constants.JIRA_PASSWORD)).toInstance(properties.getProperty(Constants.JIRA_PASSWORD));
        bind(ReviewRequestService.class).to(ReviewRequestServiceImpl.class);
        bind(SubscriptionService.class).to(SubscriptionImpl.class);
        bind(ExecutorService.class).toInstance(Executors.newFixedThreadPool(8));
        bind(ChangeInfoFormatter.class).toInstance(changeFormatter);
    }

}
