package jobs;

import java.io.IOException;
import java.util.Collection;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatDeleteRequest;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequest;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;

public class DeleteMessageJob implements Runnable
{
    private String                           changeId;
    private final App app;
    private final ReviewRequestService       reviewRequestService;

    public DeleteMessageJob(String changeId, App app, ReviewRequestService reviewRequestService)
    {
        this.changeId = changeId;
        this.app = app;
        this.reviewRequestService = reviewRequestService;
    }

    @Override
    public void run()
    {
        Collection<ReviewRequest> reviewRequests = reviewRequestService.getReviewRequests(changeId);
        for (ReviewRequest reviewRequest : reviewRequests)
        {
            try
            {
                app.getClient().chatDelete(ChatDeleteRequest.builder().channel(reviewRequest.getChannelId()).ts(reviewRequest.getLastRequestTimestamp()).build());
            }
            catch (IOException | SlackApiException e)
            {
                //throw new RuntimeException(e);
                // TODO handle exception
            }
        }
        reviewRequestService.deleteReviewRequest(changeId);
    }
}
