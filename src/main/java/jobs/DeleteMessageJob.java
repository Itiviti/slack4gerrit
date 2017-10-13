package jobs;

import java.util.Collection;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequest;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;

public class DeleteMessageJob implements Runnable
{
    private String                           changeId;
    private final SlackSession               session;
    private final ReviewRequestService       reviewRequestService;

    public DeleteMessageJob(String changeId, SlackSession session, ReviewRequestService reviewRequestService)
    {
        this.changeId = changeId;
        this.session = session;
        this.reviewRequestService = reviewRequestService;
    }

    @Override
    public void run()
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
