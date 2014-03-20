package org.atari.dhs.buzztest.server.store;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "SubscribedFeed")
@Table(name = "feeds")
@NamedQueries({
        @NamedQuery(name = "findSubscribedFeedByVerifyTokenAndVerified",
                    query = "select sf from SubscribedFeed sf where sf.verificationId = :verificationId and sf.verified = :verified"),

        @NamedQuery(name = "findSubscribedFeedByFeed",
                    query = "select sf from SubscribedFeed sf where sf.commentsUrl = :commentsUrl"),

        @NamedQuery(name = "findSubscribedFeedByActivityUrlAndC2DMKey",
                    query = "select sf from SubscribedFeed sf where sf.activityId = :activityUrl and sf.c2dmKey = :c2DMKey"),

        @NamedQuery(name = "findSubscribedFeedCountByActivityUrl",
                    query = "select count(sf) from SubscribedFeed sf where sf.activityId = :activityUrl"),

        @NamedQuery(name = "removeSubscribedFeedByActivityUrlAndC2DMKey",
                    query = "delete from SubscribedFeed sf where sf.activityId = :activityUrl and sf.c2dmKey = :c2DMKey"),

        @NamedQuery(name = "removeSubscribedFeedById",
                    query = "delete from SubscribedFeed sf where sf.id = :id")
})
public class SubscribedFeed implements Serializable
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "createdDate")
    private Date createdDate;

    @Basic
    @Column(name = "activityName")
    private String activityId;

    @Basic
    @Column(name = "feedName")
    private String commentsUrl;

    @Basic
    @Column(name = "verificationId")
    private String verificationId;

    @Basic
    @Column(name = "androidId")
    private String c2dmKey;

    @Basic
    @Column(name = "verified")
    private boolean verified;

    @Basic
    @Column(name = "hubUrl")
    private String hubUrl;

    @Basic
    @Column(name = "user")
    private String userName;

    @Basic
    @Column(name = "userId")
    private String userId;

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate( Date createdDate ) {
        this.createdDate = createdDate;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId( String activityId ) {
        this.activityId = activityId;
    }

    public String getCommentsUrl() {
        return commentsUrl;
    }

    public void setCommentsUrl( String commentsUrl ) {
        this.commentsUrl = commentsUrl;
    }

    public String getVerificationId() {
        return verificationId;
    }

    public void setVerificationId( String verificationId ) {
        this.verificationId = verificationId;
    }

    public String getC2dmKey() {
        return c2dmKey;
    }

    public void setC2dmKey( String c2dmKey ) {
        this.c2dmKey = c2dmKey;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified( boolean verified ) {
        this.verified = verified;
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl( String hubUrl ) {
        this.hubUrl = hubUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId( String userId ) {
        this.userId = userId;
    }
}
