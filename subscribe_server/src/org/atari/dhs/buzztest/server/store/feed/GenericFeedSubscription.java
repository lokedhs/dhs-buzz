package org.atari.dhs.buzztest.server.store.feed;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

@Entity(name = "GenericFeedSubscription")
@Table(name = "feed_subscription")
@NamedQueries( {
                       @NamedQuery(name = "findGenericFeedSubscriptionByExpression",
                                   query = "select object(o) from GenericFeedSubscription o where o.expression = :expression")
               })
public class GenericFeedSubscription
{
    @Id
    @Column(name = "feedUrl", updatable = false, nullable = false)
    private String feedUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", updatable = false, nullable = false)
    private SubscriptionType type;

    @Basic
    @Column(name = "hubUrl", updatable = false, nullable = false)
    private String hubUrl;

    @Basic
    @Column(name = "expression", updatable = false, nullable = false)
    private String expression;

    @Basic
    @Column(name = "verificationId", updatable = true, nullable = true)
    private String verificationId;

    @Basic
    @Column(name = "deleted", updatable = true, nullable = false)
    private boolean deleted;

    @Basic
    @Column(name = "verified", updatable = true, nullable = false)
    private boolean verified;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "createdDate", updatable = false, nullable = false)
    private Date createdDate;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private Collection<Subscriber> subscribers;

    public GenericFeedSubscription() {
    }

    public GenericFeedSubscription( String feedUrl, SubscriptionType type, String hubUrl, String expression, String verificationId, Date createdDate ) {
        this.feedUrl = feedUrl;
        this.type = type;
        this.hubUrl = hubUrl;
        this.expression = expression;
        this.verificationId = verificationId;
        this.deleted = false;
        this.verified = false;
        this.createdDate = createdDate;
        this.subscribers = new ArrayList<Subscriber>();
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl( String feedUrl ) {
        this.feedUrl = feedUrl;
    }

    public SubscriptionType getType() {
        return type;
    }

    public void setType( SubscriptionType type ) {
        this.type = type;
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl( String hubUrl ) {
        this.hubUrl = hubUrl;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression( String expression ) {
        this.expression = expression;
    }

    public String getVerificationId() {
        return verificationId;
    }

    public void setVerificationId( String verificationId ) {
        this.verificationId = verificationId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted( boolean deleted ) {
        this.deleted = deleted;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified( boolean verified ) {
        this.verified = verified;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate( Date createdDate ) {
        this.createdDate = createdDate;
    }

    public Collection<Subscriber> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers( Collection<Subscriber> subscribers ) {
        this.subscribers = subscribers;
    }

    @Override
    public String toString() {
        return "GenericFeedSubscription[" +
               "feedUrl='" + feedUrl + '\'' +
               ", type=" + type +
               ", hubUrl='" + hubUrl + '\'' +
               ", expression='" + expression + '\'' +
               ", verificationId='" + verificationId + '\'' +
               ", deleted=" + deleted +
               ", verified=" + verified +
               ", createdDate=" + createdDate +
               ']';
    }
}
