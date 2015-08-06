package com.ullink.slack.review.subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.mapdb.DB;

@Singleton
public class ProjectSubscriptionImpl implements ProjectSubscriptionService
{
    private Map<String, List<String>> projectSubscriptionMap;
    @Inject
    private DB                        db;

    private ReadWriteLock             lock = new ReentrantReadWriteLock();

    @Inject
    public ProjectSubscriptionImpl(DB db)
    {
        projectSubscriptionMap = db.<String, List<String>> getTreeMap("ProjectSubscription");
    }

    @Override
    public Collection<String> getListeningChannels(String projectName)
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
    public Collection<String> getChannelSubscriptions(String channelId)
    {
        Lock readLock = lock.readLock();
        readLock.lock();
        try
        {
            // full scan...
            Set<String> projectList = projectSubscriptionMap.keySet();
            List<String> toReturn = new ArrayList<String>();
            for (String projectId : projectList)
            {
                if (getListeningChannels(projectId).contains(channelId))
                {
                    toReturn.add(projectId);
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
            Collection<String> requestList = getListeningChannels(projectName);
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
            Collection<String> requestList = getListeningChannels(projectName);
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

}
