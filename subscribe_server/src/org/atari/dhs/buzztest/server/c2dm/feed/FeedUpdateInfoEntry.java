package org.atari.dhs.buzztest.server.c2dm.feed;

import java.io.Serializable;

public class FeedUpdateInfoEntry implements Serializable
{
    private String id;
    private String actor;
    private String actorId;
    private int numReplies;
    private long updated;
    private String content;
    private String thumbnailUrl;

    public FeedUpdateInfoEntry( String id, String actor, String actorId, int numReplies, long updated, String content, String thumbnailUrl ) {

        this.id = id;
        this.actor = actor;
        this.actorId = actorId;
        this.numReplies = numReplies;
        this.updated = updated;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
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

    public String getContent() {
        return content;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public String toString() {
        return "FeedUpdateInfoEntry[" +
               "id='" + id + '\'' +
               ", actor='" + actor + '\'' +
               ", numReplies=" + numReplies +
               ", updated='" + updated + '\'' +
               ", content='" + content + '\'' +
               ", thumbnailUrl='" + thumbnailUrl + '\'' +
               ']';
    }
}
