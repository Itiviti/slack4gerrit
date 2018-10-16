package com.ullink.slack.review.subscription;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.mapdb.DB;

@Singleton
public class SubscriptionImpl implements SubscriptionService
{
    private Map<String, List<String>> projectSubscriptionMap;
    private Map<String, List<String>> userSubscriptionMap;

    @Inject
    private DB db;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Inject
    public SubscriptionImpl(DB db)
    {
        projectSubscriptionMap = db.getTreeMap("ProjectSubscription");
        userSubscriptionMap = db.getTreeMap("UserSubscription");
    }

    @Override
    public Collection<String> getChannelsListeningToProject(String projectName)
    {
        return getSubscribedChannels(projectName, projectSubscriptionMap);
    }

    @Override
    public Collection<String> getChannelsListeningToUser(String userName)
    {
        return getSubscribedChannels(userName, userSubscriptionMap);
    }

    @Override
    public Collection<String> getChannelSubscriptions(String channelId)
    {
        return read(() ->
            Stream.concat(
                getChannelSubscriptions(channelId, projectSubscriptionMap).map(project -> "Project: " + project),
                getChannelSubscriptions(channelId, userSubscriptionMap).map(user -> "User: @" + user))
                .collect(toList())
        );
    }

    @Override
    public void subscribeOnProject(String projectName, String channelId)
    {
        subscribe(channelId, projectName, projectSubscriptionMap);
    }

    @Override
    public void subscribeOnUser(String userName, String channelId)
    {
        subscribe(channelId, userName, userSubscriptionMap);
    }

    @Override
    public void unsubscribeOnProject(String projectName, String channelId)
    {
        unsubscribe(channelId, projectName, projectSubscriptionMap);
    }

    @Override
    public void unsubscribeOnUser(String userName, String channelId)
    {
        unsubscribe(channelId, userName, userSubscriptionMap);
    }

    private void subscribe(String channelId, String key, Map<String, List<String>> subscriptions)
    {
        write(() -> {
            try
            {
                Collection<String> subscribedChannels = getSubscribedChannels(key, subscriptions);
                if (!subscribedChannels.contains(channelId))
                {
                    List<String> newList = new ArrayList<>(subscribedChannels);
                    newList.add(channelId);
                    subscriptions.put(key, newList);
                    db.commit();
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                db.rollback();
            }
        });
    }

    private void unsubscribe(String channelId, String key, Map<String, List<String>> subscriptions)
    {
        write(() -> {
            try
            {
                Collection<String> requestList = getSubscribedChannels(key, subscriptions);
                List<String> newList = new ArrayList<>(requestList);
                if (newList.removeIf(channelId::equals))
                {
                    subscriptions.put(key, newList);
                    db.commit();
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                db.rollback();
            }
        });
    }

    private Collection<String> getSubscribedChannels(String key, Map<String, List<String>> subscriptions)
    {
        return read(() ->
            Optional.ofNullable(key)
                .map(subscriptions::get)
                .<List>map(ArrayList::new)
                .orElse(emptyList())
        );
    }

    private Stream<String> getChannelSubscriptions(String channelId, Map<String, List<String>> subscriptions)
    {
        return subscriptions.entrySet().stream()
            .filter(e -> e.getValue() != null)
            .filter(e -> e.getValue().contains(channelId))
            .map(e -> e.getKey());
    }

    private void write(Runnable runnable)
    {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try
        {
            runnable.run();
        }
        finally
        {
            writeLock.unlock();
        }
    }

    private <T> T read(Supplier<T> supplier)
    {
        Lock readLock = lock.readLock();
        readLock.lock();
        try
        {
            return supplier.get();
        }
        finally
        {
            readLock.unlock();
        }
    }
}
