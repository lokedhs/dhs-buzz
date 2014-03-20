package org.atari.dhs.buzztest.androidclient.buzz.jsonmodel;

import java.io.Serializable;
import java.util.List;

import com.google.api.client.util.Key;
import org.atari.dhs.buzztest.androidclient.Log;

public final class BuzzActivity implements Serializable
{
    @Key("id")
    public String id;

    @Key("title")
    public String title;

    @Key("updated")
    public String updated;

    @Key("object")
    public BuzzObject object;

    @Key("links")
    public Links links;

    @Key("actor")
    public Actor actor;

    @Key("verbs")
    public List<String> verbs;

    @Key("source")
    public BuzzActivitySource source;

    @Key("crosspostSource")
    public String crosspostSource;

    @Key("geocode")
    public String geocode;

    @Key("address")
    public String address;

    @Key("placeName")
    public String placeName;

    @Key("placeId")
    public String placeId;

    @Key("annotation")
    public String annotation;

    @Key("visibility")
    public VisibilitySettings visibility;

    @Override
    public String toString() {
        return "BuzzActivity[" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", updated='" + updated + '\'' +
               ", object=" + object +
               ", links=" + links +
               ", actor=" + actor +
               ", verbs=" + verbs +
               ", source=" + source +
               ", crosspostSource='" + crosspostSource + '\'' +
               ", geocode='" + geocode + '\'' +
               ", address='" + address + '\'' +
               ", placeName='" + placeName + '\'' +
               ", placeId='" + placeId + '\'' +
               ", annotation='" + annotation + '\'' +
               ']';
    }

    public int findReplyCount() {
        Replies replies = findReplies();
        return replies == null ? 0 : replies.count;
    }

    public Replies findReplies() {
        if( links == null || links.replies == null || links.replies.isEmpty() ) {
            return null;
        }
        else {
            return links.replies.get( 0 );
        }
    }

    public String findFrom() {
        return source == null ? "" : source.title;
    }

    public String findSelfLink() {
        if( links == null || links.self == null || links.self.isEmpty() ) {
            throw new IllegalStateException( "missing self link" );
        }
        if( links.self.size() > 1 ) {
            Log.w( "More than one self link, returning the first one" );
        }
        return links.self.get( 0 ).href;
    }

    public String findAlternateLinkOfType( String type ) {
        if( links == null || links.alternate == null ) {
            return null;
        }
        for( Alternate alt : links.alternate ) {
            if( alt.type.equals( type ) ) {
                return alt.href;
            }
        }
        return null;
    }

    public boolean hasVerb( String verb ) {
        return verbs != null && verbs.contains( verb );
    }

    public boolean isReshare() {
        return hasVerb( "share" );
    }
}
