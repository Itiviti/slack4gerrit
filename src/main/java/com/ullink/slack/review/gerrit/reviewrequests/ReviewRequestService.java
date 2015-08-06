package com.ullink.slack.review.gerrit.reviewrequests;

import java.util.Collection;

public interface ReviewRequestService
{
    ReviewRequest getReviewRequest(String channel, String requestId);
    Collection<ReviewRequest> getReviewRequests(String requestId);
    void registerReviewRequest(ReviewRequest reviewRequest);
    Collection<String> getAllRequestedReviews();
    void deleteReviewRequest(String requestId);
}
