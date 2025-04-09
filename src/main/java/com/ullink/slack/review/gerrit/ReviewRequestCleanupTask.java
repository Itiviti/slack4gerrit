package com.ullink.slack.review.gerrit;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import jobs.DeleteMessageJob;
import jobs.RefreshMessageJob;
import com.slack.api.bolt.App;
import com.ullink.slack.review.gerrit.reviewrequests.ReviewRequestService;

public class ReviewRequestCleanupTask implements Runnable
{

    private ReviewRequestService    reviewRequestService;
    private GerritChangeInfoService gerritChangeInfoService;
    private App app;
    private ExecutorService executorService;
    private ChangeInfoFormatter changeInfoDecorator;

    public ReviewRequestCleanupTask(ReviewRequestService reviewRequestService, GerritChangeInfoService gerritChangeInfoService, ChangeInfoFormatter changeInfoDecorator, App app, ExecutorService executorService)
    {
        this.app = app;
        this.reviewRequestService = reviewRequestService;
        this.gerritChangeInfoService = gerritChangeInfoService;
        this.executorService = executorService;
        this.changeInfoDecorator = changeInfoDecorator;
    }

    @Override
    public void run()
    {
        Collection<String> pendingChanges = reviewRequestService.getAllRequestedReviews();
        for (String changeId : pendingChanges)
        {
            try
            {
                if (gerritChangeInfoService.isMergedOrAbandoned(changeId))
                {
                    executorService.submit(new DeleteMessageJob(changeId, app, reviewRequestService));
                }
                else
                {
                    executorService.submit(new RefreshMessageJob(changeId, app, reviewRequestService, gerritChangeInfoService, changeInfoDecorator));
                }
            } catch (RuntimeException e) {
                // DO NOTHING, error was logged
            }
        }
    }

}
