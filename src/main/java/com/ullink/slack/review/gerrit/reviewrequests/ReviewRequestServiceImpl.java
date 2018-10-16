package com.ullink.slack.review.gerrit.reviewrequests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.mapdb.DB;

@Singleton
public class ReviewRequestServiceImpl implements ReviewRequestService
{
    private Map<String, List<ReviewRequest>> reviewRequestMap;
    @Inject
    private DB db;

    @Inject
    public ReviewRequestServiceImpl(DB db)
    {
        reviewRequestMap = db.getTreeMap("ReviewRequests");
    }

    @Override
    public ReviewRequest getReviewRequest(String channel, String requestId)
    {
        if (reviewRequestMap.containsKey(requestId))
        {
            List<ReviewRequest> requestList = reviewRequestMap.get(requestId);
            if (requestList != null)
            {
                for (ReviewRequest request : requestList)
                {
                    if (channel.equals(request.getChannelId()))
                    {
                        return request;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Collection<ReviewRequest> getReviewRequests(String requestId)
    {
        List<ReviewRequest> requestList = reviewRequestMap.get(requestId);
        if (requestList != null)
        {
            return new ArrayList<>(requestList);
        }
        return Collections.emptyList();
    }

    @Override
    public void registerReviewRequest(ReviewRequest reviewRequest)
    {
        try
        {
            Collection<ReviewRequest> requestList = getReviewRequests(reviewRequest.getChangeId());
            List<ReviewRequest> newList = new ArrayList<>(requestList);
            for (Iterator<ReviewRequest> reviewRequestIterator = newList.iterator(); reviewRequestIterator.hasNext(); )
            {
                ReviewRequest request = reviewRequestIterator.next();
                if (reviewRequest.getChannelId().equals(request.getChannelId()))
                {
                    reviewRequestIterator.remove();
                }
            }
            newList.add(reviewRequest);
            reviewRequestMap.put(reviewRequest.getChangeId(), newList);
            db.commit();
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            db.rollback();
        }
    }

    @Override
    public Collection<String> getAllRequestedReviews()
    {
        return new ArrayList<>(reviewRequestMap.keySet());
    }

    @Override
    public void deleteReviewRequest(String requestId)
    {
        if (reviewRequestMap.containsKey(requestId))
        {
            try
            {
                reviewRequestMap.remove(requestId);
                db.commit();
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                db.rollback();
            }
        }
    }

}
