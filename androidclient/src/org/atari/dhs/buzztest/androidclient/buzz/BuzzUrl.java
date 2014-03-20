package org.atari.dhs.buzztest.androidclient.buzz;

import android.net.Uri;

import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.util.Key;
import com.google.common.base.Joiner;

import org.atari.dhs.buzztest.androidclient.DhsBuzzApp;

public final class BuzzUrl extends GoogleUrl
{
    /**
     * Debug flag. Enabling will show HTTP request/response details.
     */
    public static final boolean DEBUG = false;

    @Key
    public String key;

    /**
     * Constructs a new Buzz URL from the given encoded URI.
     *
     * @param encodedUrl the encoded url
     */
    public BuzzUrl( String encodedUrl )
    {
        super( encodedUrl );
        alt = "json";
        if( DEBUG ) {
            prettyprint = true;
        }
        key = DhsBuzzApp.API_KEY;
    }

    /**
     * Constructs a new Buzz URL based on the given relative path.
     *
     * @param relativePath unencoded path relative to the {@link BuzzGlobal#ROOT_URL},
     * but not containing any query parameters
     * @return new Buzz URL
     */
    public static BuzzUrl fromRelativePath( String relativePath )
    {
        BuzzUrl result = new BuzzUrl( BuzzGlobal.ROOT_URL + relativePath );
        return result;
    }

    public static String buildActivityUrl( String userId, String activityId ) {
        StringBuilder buf = new StringBuilder();
        buf.append( BuzzGlobal.ROOT_URL );
        buf.append( "activities/" );
        if( userId == null ) {
            buf.append( "@me" );
        }
        else {
            buf.append( userId );
        }
        buf.append( "/@self/" );
        buf.append( activityId );
        return buf.toString();
    }

    public static String buildCommentsUrl( String userId, String activityId, String replyId ) {
        StringBuilder buf = new StringBuilder();
        buf.append( BuzzGlobal.ROOT_URL );
        buf.append( "activities/" );
        if( userId == null ) {
            buf.append( "@me" );
        }
        else {
            buf.append( userId );
        }
        buf.append( "/@self/" );
        buf.append( activityId );
        buf.append( "/@comments" );
        if( replyId != null ) {
            buf.append( "/" );
            buf.append( replyId );
        }
        return buf.toString();
    }

    public static String buildSearchUrlFromParts( String... searchWords ) {
        return buildSearchUrl( Joiner.on( " " ).join( searchWords ) );
    }

    public static String buildSearchUrl( String expression ) {
        return BuzzGlobal.ROOT_URL + "activities/search?q=" + Uri.encode( expression );
    }
}
