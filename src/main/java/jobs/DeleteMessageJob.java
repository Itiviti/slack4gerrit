package jobs;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequest;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;

public class DeleteMessageJob implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteMessageJob.class);

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
        try
        {
            Collection<ReviewRequest> reviewRequests = reviewRequestService.getReviewRequests(changeId);
            for (ReviewRequest reviewRequest : reviewRequests)
            {
                SlackChannel channel = session.findChannelById(reviewRequest.getChannelId());
                if (channel != null)
                {
                    session.deleteMessage(reviewRequest.getLastRequestTimestamp(), channel);
                }
                else
                {
                    LOGGER.warn("Cannot find channel " + reviewRequest.getChannelId() + " for delete request " + reviewRequest + " with changeId " + reviewRequest.getChangeId());
                }
            }
            reviewRequestService.deleteReviewRequest(changeId);
        }
        catch (Throwable t)
        {
            LOGGER.error("Unexpected error, t");
        }
    }
}
