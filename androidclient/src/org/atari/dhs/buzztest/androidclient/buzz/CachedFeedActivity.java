package org.atari.dhs.buzztest.androidclient.buzz;

import java.io.Serializable;

import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class CachedFeedActivity implements Serializable
{
    private String id;
    private String actor;
    private String actorId;
    private int numReplies;
    private long updated;
    private String title;
    private String thumbnailUrl;

    public CachedFeedActivity( String id, String actor, String actorId, int numReplies, long updated, String title, String thumbnailUrl ) {
        this.id = id;
        this.actor = actor;
        this.actorId = actorId;
        this.numReplies = numReplies;
        this.updated = updated;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
    }

    public CachedFeedActivity( BuzzActivity act, DateHelper dateHelper ) {
        id = act.id;
        actor = act.actor.name;
        actorId = act.actor.id;
        numReplies = act.findReplyCount();
        updated = dateHelper.parseDate( act.updated ).getTime();
        title = act.title;
        thumbnailUrl = act.actor.thumbnailUrl;
    }

    public String getId() {
        return id;
    }

    public String getActor() {
        return actor;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId( String actorId ) {
        this.actorId = actorId;
    }

    public int getNumReplies() {
        return numReplies;
    }

    public long getUpdated() {
        return updated;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
