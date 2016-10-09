package com.ullink.slack.review.subscription;

import org.mapdb.DB;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class SubscriptionImpl implements SubscriptionService
{
    private Map<String, List<String>> projectSubscriptionMap;
    private Map<String, List<String>> userSubscriptionMap;
    @Inject
    private DB                        db;

    private ReadWriteLock             lock = new ReentrantReadWriteLock();

    @Inject
    public SubscriptionImpl(DB db)
    {
        projectSubscriptionMap = db.<String, List<String>> getTreeMap("ProjectSubscription");
        userSubscriptionMap = db.<String, List<String>> getTreeMap("UserSubscription");
    }

    @Override
    public Collection<String> getChannelsListeningToProject(String projectName)
    {
        Lock readLock = lock.readLock();
        readLock.lock();
        try
        {
            List<String> channelList = projectSubscriptionMap.get(projectName);
            if (channelList != null)
            {
                return new ArrayList<String>(channelList);
            }
        }
        finally
        {
            readLock.unlock();
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getChannelsListeningToUser(String userName)
    {
        Lock readLock = lock.readLock();
        readLock.lock();
        try
        {
            List<String> channelList = userSubscriptionMap.get(userName);
            if (channelList != null)
            {
                return new ArrayList<String>(channelList);
            }
        }
        finally
        {
            readLock.unlock();
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getChannelSubscriptions(String channelId)
    {
        Lock readLock = lock.readLock();
        readLock.lock();
        try
        {
            // full scan...
            Set<String> projectList = projectSubscriptionMap.keySet();
            Set<String> userList = userSubscriptionMap.keySet();
            List<String> toReturn = new ArrayList<String>();
            for (String projectId : projectList)
            {
                if (getChannelsListeningToProject(projectId).contains(channelId))
                {
                    toReturn.add("Project: " + projectId);
                }
            }
            for (String userId : userList)
            {
                if (getChannelsListeningToUser(userId).contains(channelId))
                {
                    toReturn.add("User: @" + userId);
                }
            }
            return toReturn;
        }
        finally
        {
            readLock.unlock();
        }
    }

    @Override
    public void subscribeOnProject(String projectName, String channelId)
    {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try
        {
            Collection<String> requestList = getChannelsListeningToProject(projectName);
            ArrayList<String> newList = new ArrayList<String>(requestList);
            for (String subscribingChannelId : newList)
            {
                if (channelId.equals(subscribingChannelId))
                {
                    // already subscribed
                    return;
                }
            }
            newList.add(channelId);
            projectSubscriptionMap.put(projectName, newList);
            db.commit();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            db.rollback();
        }
        finally
        {
            writeLock.unlock();
        }
    }

    @Override
    public void unsubscribeOnProject(String projectName, String channelId)
    {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try
        {
            Collection<String> requestList = getChannelsListeningToProject(projectName);
            ArrayList<String> newList = new ArrayList<String>(requestList);
            for (Iterator<String> channelIterator = newList.iterator(); channelIterator.hasNext();)
            {
                String subscribingChannelId = channelIterator.next();
                if (channelId.equals(subscribingChannelId))
                {
                    channelIterator.remove();
                }
            }
            projectSubscriptionMap.put(projectName, newList);
            db.commit();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            db.rollback();
        }
        finally
        {
            writeLock.unlock();
        }
    }

    @Override
    public void subscribeOnUser(String userName, String channelId)
    {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try
        {
            Collection<String> requestList = getChannelsListeningToUser(userName);
            ArrayList<String> newList = new ArrayList<String>(requestList);
            for (String subscribingChannelId : newList)
            {
                if (channelId.equals(subscribingChannelId))
                {
                    // already subscribed
                    return;
                }
            }
            newList.add(channelId);
            userSubscriptionMap.put(userName, newList);
            db.commit();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            db.rollback();
        }
        finally
        {
            writeLock.unlock();
        }
    }

    @Override
    public void unsubscribeOnUser(String userName, String channelId){
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try
        {
            Collection<String> requestList = getChannelsListeningToUser(userName);
            ArrayList<String> newList = new ArrayList<String>(requestList);
            for (Iterator<String> channelIterator = newList.iterator(); channelIterator.hasNext();)
            {
                String subscribingChannelId = channelIterator.next();
                if (channelId.equals(subscribingChannelId))
                {
                    channelIterator.remove();
                }
            }
            userSubscriptionMap.put(userName, newList);
            db.commit();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            db.rollback();
        }
        finally
        {
            writeLock.unlock();
        }
    }
}
