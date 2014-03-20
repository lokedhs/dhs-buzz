package org.atari.dhs.buzztest.server.store.feed;

import javax.persistence.*;
import java.util.Date;

import com.google.appengine.api.datastore.Key;

@Entity(name = "subscription")
@Table(name = "subscription")
@NamedQueries( {
                       @NamedQuery(name = "findSubscriberByParentAndC2DMKey",
                                   query = "select object(o) from subscription o where o.parent = :parent and o.c2dmKey = :c2dmKey"),
                       @NamedQuery(name = "findSubscriberByExpressionAndC2DMKey",
                                   query = "select o from subscription o where o.parent.expression = :expression and o.c2dmKey = :c2dmKey"),
                       @NamedQuery(name = "findSubscriberBySubscriptionIdAndC2DMKey",
                                   query = "select o from subscription o where o.subscriptionId = :subscriptionId and o.c2dmKey = :c2dmKey")
               })
public class Subscriber
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Key id;

    @Basic
    @Column(name = "c2dmKey", updatable = false, nullable = false)
    private String c2dmKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent", updatable = false, nullable = false)
    private GenericFeedSubscription parent;

    @Basic
    @Column(name = "username", updatable = false, nullable = true)
    private String username;

    @Basic
    @Column(name = "userId", updatable = false, nullable = false)
    private String userId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "createdDate", updatable = false, nullable = false)
    private Date createdDate;

    @Basic
    @Column(name = "subscriptionId", updatable = false, nullable = false)
    private String subscriptionId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "lastRefreshDate", updatable = true, nullable = false)
    private Date lastRefreshDate;

    public Subscriber() {
    }

    public Subscriber( String c2dmKey, GenericFeedSubscription parent, String username, String userId, Date createdDate, String subscriptionId ) {
//        this.c2dmKey = KeyFactory.createKey( KeyFactory.createKey( GenericFeedSubscription.class.getSimpleName(), parent.getFeedUrl() ), Subscriber.class.getSimpleName(), c2dmKey );
//        this.id = new KeyFactory.Builder( GenericFeedSubscription.class.getSimpleName(), parent.getFeedUrl() ).addChild( Subscriber.class.getSimpleName(), c2dmKey ).getKey();
        this.c2dmKey = c2dmKey;
        this.parent = parent;
        this.username = username;
        this.userId = userId;
        this.createdDate = createdDate;
        this.lastRefreshDate = createdDate;
        this.subscriptionId = subscriptionId;
        this.lastRefreshDate = new Date();
    }

//    public Key getId() {
//        return id;
//    }
//
//    public void setId( Key id ) {
//        this.id = id;
//    }


    public Key getId() {
        return id;
    }

    public void setId( Key id ) {
        this.id = id;
    }

    public String getC2dmKey() {
        return c2dmKey;
    }

    public void setC2dmKey( String c2dmKey ) {
        this.c2dmKey = c2dmKey;
    }

    public GenericFeedSubscription getParent() {
        return parent;
    }

    public void setParent( GenericFeedSubscription parent ) {
        this.parent = parent;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername( String username ) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId( String userId ) {
        this.userId = userId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate( Date createdDate ) {
        this.createdDate = createdDate;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId( String subscriptionId ) {
        this.subscriptionId = subscriptionId;
    }

    public Date getLastRefreshDate() {
        return lastRefreshDate;
    }

    public void setLastRefreshDate( Date lastRefreshDate ) {
        this.lastRefreshDate = lastRefreshDate;
    }

    @Override
    public String toString() {
        return "Subscriber[" +
               "id=" + id +
               ", c2dmKey='" + c2dmKey + '\'' +
               ", parent=" + parent +
               ", username='" + username + '\'' +
               ", userId='" + userId + '\'' +
               ", createdDate=" + createdDate +
               ", subscriptionId='" + subscriptionId + '\'' +
               ", lastRefreshDate=" + lastRefreshDate +
               ']';
    }
}
