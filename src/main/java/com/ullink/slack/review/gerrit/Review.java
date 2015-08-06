package com.ullink.slack.review.gerrit;

public class Review
{
    private String reviewer;
    private String reviewerEmail;    
    private int reviewValue;
    
    public String getReviewerEmail()
    {
        return reviewerEmail;
    }
    public void setReviewerEmail(String reviewerEmail)
    {
        this.reviewerEmail = reviewerEmail;
    }
    
    public String getReviewer()
    {
        return reviewer;
    }
    public void setReviewer(String reviewer)
    {
        this.reviewer = reviewer;
    }
    public int getReviewValue()
    {
        return reviewValue;
    }
    public void setReviewValue(int reviewValue)
    {
        this.reviewValue = reviewValue;
    }
    
}
