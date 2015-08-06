package com.ullink.slack.review.gerrit;

import java.util.Collection;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequest;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;

public class ReviewRequestCleanupTask implements Runnable
{

    private ReviewRequestService    reviewRequestService;
    private GerritChangeInfoService gerritChangeInfoService;
    private SlackSession            session;

    public ReviewRequestCleanupTask(ReviewRequestService reviewRequestService, GerritChangeInfoService gerritChangeInfoService, SlackSession session)
    {
        this.session = session;
        this.reviewRequestService = reviewRequestService;
        this.gerritChangeInfoService = gerritChangeInfoService;
    }

    @Override
    public void run()
    {
        Collection<String> pendingChanges = reviewRequestService.getAllRequestedReviews();
        for (String changeId : pendingChanges)
        {
            if (gerritChangeInfoService.isMerged(changeId))
            {
                Collection<ReviewRequest> reviewRequests = reviewRequestService.getReviewRequests(changeId);
                for (ReviewRequest reviewRequest : reviewRequests)
                {
                    SlackChannel channel = session.findChannelById(reviewRequest.getChannelId());
                    if (channel != null)
                    {
                        session.deleteMessage(reviewRequest.getLastRequestTimestamp(), channel);
                    }
                }
                reviewRequestService.deleteReviewRequest(changeId);
            }
        }
    }

}
