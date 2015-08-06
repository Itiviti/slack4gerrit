package com.ullink.slack.review.gerrit;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChangeInfo
{
    public enum IssuePriority
    {
        BLOCKER, CRITICAL, MAJOR, MINOR, UNKNOWN;

        static IssuePriority fromString(String value)
        {
            if ("Minor".equals(value))
            {
                return MINOR;
            }
            else if ("Major".equals(value))
            {
                return MAJOR;
            }
            else if ("Critical".equals(value))
            {
                return CRITICAL;
            }
            else if ("Blocker".equals(value))
            {
                return BLOCKER;
            }
            else
            {
                return UNKNOWN;
            }
        }
    }

    public enum IssueType
    {
        BUG, IMPROVEMENT, QUESTION, UNKNOWN;

        static IssueType fromString(String value)
        {
            if ("Bug".equals(value))
            {
                return BUG;
            }
            else if ("Improvement".equals(value))
            {
                return IMPROVEMENT;
            }
            else if ("Question".equals(value))
            {
                return QUESTION;
            }
            else
            {
                return UNKNOWN;
            }
        }
    }

    private String                owner;
    private String                ownerEmail;
    private String                project;
    private String                branch;
    private String                subject;
    private String                changeId;
    private String                id;
    private String                cherryPickedFrom;
    private Date                  created;
    private Date                  updated;
    private int                   insertion;
    private int                   deletion;
    private String                commitMessage;
    private Map<String, JIRAInfo> relatedJira = new HashMap<String, JIRAInfo>();

    public Map<String, JIRAInfo> getRelatedJira()
    {
        return relatedJira;
    }

    public void setRelatedJira(Map<String, JIRAInfo> relatedJira)
    {
        this.relatedJira = relatedJira;
    }

    private Map<String, Set<Review>> reviews = new HashMap<String, Set<Review>>();

    public Map<String, Set<Review>> getReviews()
    {
        return reviews;
    }

    public void setReviews(Map<String, Set<Review>> reviews)
    {
        this.reviews = reviews;
    }

    public String getOwner()
    {
        return owner;
    }

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getOwnerEmail()
    {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail)
    {
        this.ownerEmail = ownerEmail;
    }

    public String getProject()
    {
        return project;
    }

    public void setProject(String project)
    {
        this.project = project;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date created)
    {
        this.created = created;
    }

    public Date getUpdated()
    {
        return updated;
    }

    public void setUpdated(Date updated)
    {
        this.updated = updated;
    }

    public int getInsertion()
    {
        return insertion;
    }

    public void setInsertion(int insertion)
    {
        this.insertion = insertion;
    }

    public int getDeletion()
    {
        return deletion;
    }

    public void setDeletion(int deletion)
    {
        this.deletion = deletion;
    }

    public String getCommitMessage()
    {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage)
    {
        this.commitMessage = commitMessage;
    }

    public String getChangeId()
    {
        return changeId;
    }

    public void setChangeId(String changeId)
    {
        this.changeId = changeId;
    }

    public void setCherryPickedFrom(String cherryPickedFrom)
    {
        this.cherryPickedFrom = cherryPickedFrom;
    }

    public String getCherryPickedFrom()
    {
        return cherryPickedFrom;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }
}
