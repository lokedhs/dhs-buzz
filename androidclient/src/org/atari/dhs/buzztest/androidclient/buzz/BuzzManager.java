package org.atari.dhs.buzztest.androidclient.buzz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.api.client.auth.oauth.OAuthHmacSigner;
import com.google.api.client.auth.oauth.OAuthParameters;
import com.google.api.client.googleapis.GoogleTransport;
import com.google.api.client.googleapis.json.JsonCContent;
import com.google.api.client.googleapis.json.JsonCParser;
import com.google.api.client.http.*;

import org.atari.dhs.buzztest.androidclient.*;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivityFeed;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.groups.GroupsResult;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply.BuzzReply;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply.BuzzReplyFeed;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.User;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.UsersList;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;
import org.atari.dhs.buzztest.androidclient.tools.SerialisationHelper;

public class BuzzManager
{
    private Context context;
    private HttpTransport transport;
    private SQLiteDatabaseWrapper db;

    private BuzzManager( Context context, String token, String tokenSecret ) {
        this.context = context;

        transport = GoogleTransport.create();
        transport.addParser( new JsonCParser() );

        OAuthParameters params = createOAuthParameters( token, tokenSecret );
        params.signRequestsUsingAuthorizationHeader( transport );
    }

    public static BuzzManager createFromContext( Context context ) {
        PreferencesManager prefs = new PreferencesManager( context );
        String token = prefs.getBuzzToken();
        String secret = prefs.getBuzzSecret();
        if( token == null || secret == null ) {
            throw new IllegalStateException( "token or secret is null" );
        }

        return new BuzzManager( context, token, secret );
    }

    public static BuzzManager createFromKey( Context context, String token, String tokenSecret ) {
        return new BuzzManager( context, token, tokenSecret );
    }

    private static OAuthParameters createOAuthParameters( String token, String tokenSecret ) {
        OAuthParameters authoriser = new OAuthParameters();
        authoriser.consumerKey = "anonymous";

        OAuthHmacSigner signer = new OAuthHmacSigner();
        signer.tokenSharedSecret = tokenSecret;
        signer.clientSharedSecret = "anonymous";

        authoriser.signer = signer;
        authoriser.token = token;

        return authoriser;
    }

    public static String makeLocationSearchUrl( double latitude, double longitude, int radius, int maxResults ) {
        StringBuilder buf = new StringBuilder();
        buf.append( BuzzGlobal.ROOT_URL );
        buf.append( "activities/search?lat=" );
        buf.append( latitude );
        buf.append( "&lon=" );
        buf.append( longitude );
        buf.append( "&radius=" );
        buf.append( radius );
        if( maxResults > 0 ) {
            buf.append( "&max-results=" );
            buf.append( maxResults );
        }
        return buf.toString();
    }

    public static String makeUserIdFeedUrl( String userId ) {
        StringBuilder buf = new StringBuilder();
        buf.append( BuzzGlobal.ROOT_URL );
        buf.append( "activities/" );
        buf.append( userId );
        buf.append( "/@public" );
        return buf.toString();
    }

    public void close() {
        if( db != null ) {
            db.close();
            db = null;
        }
    }

    public LoadActivitiesResult loadActivities( String storedFeedKey, String activityUrl ) throws IOException {
        long lastRead = 0;
        BuzzActivityFeed feed = loadAndParse( activityUrl, BuzzActivityFeed.class );
        if( storedFeedKey != null ) {
            lastRead = insertFeedInDatabase( storedFeedKey, feed );
        }
        return new LoadActivitiesResult( feed, lastRead );
    }

    public boolean loadMore( String storedFeedKey, String activityUrl ) throws IOException, ClassNotFoundException {
        SQLiteDatabase db = getDatabase();

        Cursor result = db.query( StorageHelper.FEED_CACHE_TABLE,
                                  new String[] { StorageHelper.FEED_CACHE_CONTENT },
                                  StorageHelper.FEED_CACHE_ID + " = ?", new String[] { storedFeedKey },
                                  null, null,
                                  null );

        CachedFeed rootFeed = null;
        if( result.moveToNext() ) {
            rootFeed = (CachedFeed)SerialisationHelper.deserialiseObject( result.getBlob( 0 ) );
        }
        result.close();

        if( rootFeed == null ) {
            loadActivities( storedFeedKey, activityUrl );
            return true;
        }

        String nextLink = rootFeed.getLoadNextLink();
        if( nextLink == null ) {
            return false;
        }

        BuzzActivityFeed feed = loadAndParse( nextLink, BuzzActivityFeed.class );
        DateHelper dateHelper = new DateHelper();
        rootFeed.addActivities( feed.activities, feed.findNextLink() );
        ContentValues values = new ContentValues();
        values.put( StorageHelper.FEED_CACHE_CONTENT, SerialisationHelper.serialiseObject( rootFeed ) );
        db.update( StorageHelper.FEED_CACHE_TABLE,
                   values,
                   StorageHelper.FEED_CACHE_ID + " = ?", new String[] { storedFeedKey } );

        long now = System.currentTimeMillis();

        for( BuzzActivity activity : feed.activities ) {
            saveActivity( db, false, now, dateHelper, activity );
        }

        return true;
    }

    public BuzzActivity loadActivity( String activityId, boolean forceSaveInDatabase ) throws IOException {
        long now = System.currentTimeMillis();

        String url = BuzzUrl.buildActivityUrl( null, activityId );
        BuzzActivity result = loadAndParse( url, BuzzActivity.class );

        SQLiteDatabase db = getDatabase();
        saveActivity( db, forceSaveInDatabase, now, new DateHelper(), result );

        ContentValues values = new ContentValues();
        values.put( StorageHelper.MESSAGE_CACHE_LAST_READ_DATE, now );
        db.update( StorageHelper.MESSAGE_CACHE_TABLE,
                   values,
                   StorageHelper.MESSAGE_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId } );

        return result;
    }

    public static String getConsumptionFeedUrl() {
//        return BuzzGlobal.ROOT_URL + "activities/@me/@consumption?max-results=40";
        return BuzzGlobal.ROOT_URL + "activities/@me/@consumption";
    }

    private long insertFeedInDatabase( String feedName, BuzzActivityFeed feed ) {
        long now = System.currentTimeMillis();

        SQLiteDatabase db = getDatabase();

        DateHelper dateHelper = new DateHelper();

        ContentValues values = new ContentValues();
        values.put( StorageHelper.FEED_CACHE_ID, feedName );
        values.put( StorageHelper.FEED_CACHE_CREATED_DATE, dateHelper.parseDate( feed.updated ).getTime() );
        values.put( StorageHelper.FEED_CACHE_DOWNLOAD_DATE, now );
        values.put( StorageHelper.FEED_CACHE_CONTENT, SerialisationHelper.serialiseObject( new CachedFeed( feed ) ) );

        db.beginTransaction();
        try {
            Cursor result = db.query( StorageHelper.FEED_CACHE_TABLE,
                                      new String[] { StorageHelper.FEED_CACHE_LAST_READ_DATE },
                                      StorageHelper.FEED_CACHE_ID + " = ?", new String[] { feedName },
                                      null, null,
                                      null );
            boolean wasFound = false;
            long lastRead = 0;
            if( result.moveToNext() ) {
                lastRead = result.getLong( 0 );
                wasFound = true;
            }

            result.close();

            if( wasFound ) {
                db.update( StorageHelper.FEED_CACHE_TABLE,
                           values,
                           StorageHelper.FEED_CACHE_ID + " = ?", new String[] { feedName } );
            }
            else {
                db.insert( StorageHelper.FEED_CACHE_TABLE,
                           StorageHelper.FEED_CACHE_CONTENT,
                           values );
            }

            if( feed.activities != null ) {
                for( BuzzActivity activity : feed.activities ) {
                    saveActivity( db, false, now, dateHelper, activity );
                }
            }
            db.setTransactionSuccessful();

            return lastRead;
        }
        finally {
            db.endTransaction();
        }
    }

    public void saveActivity( SQLiteDatabase db, boolean force, long now, DateHelper dateHelper, BuzzActivity activity ) {
        String activityId = activity.id;

        //        long mainUpdated = dateHelper.parseDate( updated ).getTime();
//        long repliesUpdated = dateHelper.parseDate( findReplies().updated ).getTime();
//        long updated = Math.max( mainUpdated, repliesUpdated );
//        return updated;
        long updated = dateHelper.parseDate( activity.findReplies().updated ).getTime();
        Cursor result = db.query( StorageHelper.MESSAGE_CACHE_TABLE,
                                  new String[] { StorageHelper.MESSAGE_CACHE_UPDATED },
                                  StorageHelper.MESSAGE_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId },
                                  null, null,
                                  null );

        boolean hasRow = false;
        boolean skipUpdate = false;
        if( result.moveToNext() ) {
            hasRow = true;
            long updatedInDb = result.getLong( 0 );
            if( !force && updatedInDb == updated ) {
                skipUpdate = true;
            }
        }
        result.close();

        if( !skipUpdate ) {
            if( hasRow ) {
                ContentValues values = new ContentValues();
                values.put( StorageHelper.MESSAGE_CACHE_UPDATED, updated );
                values.put( StorageHelper.MESSAGE_CACHE_DOWNLOAD_DATE, now );
                values.put( StorageHelper.MESSAGE_CACHE_TITLE, activity.title == null ? "" : activity.title );
                values.put( StorageHelper.MESSAGE_CACHE_CONTENT, SerialisationHelper.serialiseObject( activity ) );
                int n = db.update( StorageHelper.MESSAGE_CACHE_TABLE,
                                   values,
                                   StorageHelper.MESSAGE_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId } );
            }
            else {
                ContentValues values = new ContentValues();
                values.put( StorageHelper.MESSAGE_CACHE_ACTIVITY_NAME, activityId );
                values.put( StorageHelper.MESSAGE_CACHE_DOWNLOAD_DATE, now );
                values.put( StorageHelper.MESSAGE_CACHE_UPDATED, updated );
                values.put( StorageHelper.MESSAGE_CACHE_TITLE, activity.title == null ? "" : activity.title );
                values.put( StorageHelper.MESSAGE_CACHE_CONTENT, SerialisationHelper.serialiseObject( activity ) );
                values.put( StorageHelper.MESSAGE_CACHE_LAST_READ_DATE, 0 );
                db.insert( StorageHelper.MESSAGE_CACHE_TABLE, StorageHelper.MESSAGE_CACHE_CONTENT, values );
            }
        }
    }

    public PersistedFeedWrapper loadFeedFromDatabase( String storedFeedKey ) {
        SQLiteDatabase db = getDatabase();
        Cursor result = db.query( StorageHelper.FEED_CACHE_TABLE,
                                  new String[] { StorageHelper.FEED_CACHE_DOWNLOAD_DATE, StorageHelper.FEED_CACHE_CONTENT },
                                  StorageHelper.FEED_CACHE_ID + " = ?", new String[] { storedFeedKey },
                                  null,
                                  null,
                                  null );
        long downloadDate;
        byte[] buf;
        try {
            if( !result.moveToNext() ) {
                return null;
            }
            downloadDate = result.getLong( 0 );
            buf = result.getBlob( 1 );
        }
        finally {
            result.close();
        }

        CachedFeed feed;
        try {
            feed = (CachedFeed)SerialisationHelper.deserialiseObject( buf );
        }
        catch( IOException e ) {
            Log.e( "exception when deserialising cached object, reloading from source", e );
            return null;
        }
        catch( ClassNotFoundException e ) {
            Log.e( "exception when deserialising cached object, reloading from source", e );
            return null;
        }
        return new PersistedFeedWrapper( feed, downloadDate );
    }

    public BuzzReplyFeed loadCachedRepliesIfAvailable( String activityId, long feedUpdateDate, boolean failIfCacheOutdated ) throws IOException {
        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        try {
            Cursor result = db.query( StorageHelper.REPLIES_CACHE_TABLE,
                                      new String[] { StorageHelper.REPLIES_CACHE_UPDATED_DATE },
                                      StorageHelper.REPLIES_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId },
                                      null,
                                      null,
                                      null );
            long cachedDate = 0;
            if( result.moveToNext() ) {
                cachedDate = result.getLong( 0 );
            }
            result.close();

            if( cachedDate != 0 && feedUpdateDate == cachedDate ) {
                result = db.query( StorageHelper.REPLIES_CACHE_TABLE,
                                   new String[] { StorageHelper.REPLIES_CACHE_CONTENT },
                                   StorageHelper.REPLIES_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId },
                                   null,
                                   null,
                                   null );
                if( !result.moveToNext() ) {
                    result.close();
                    throw new IllegalStateException( "cached replies for activity did not exist: " + activityId );
                }

                byte[] buf = result.getBlob( 0 );
                result.close();
                try {
                    return (BuzzReplyFeed)SerialisationHelper.deserialiseObject( buf );
                }
                catch( IOException e ) {
                    Log.e( "exception when deserialising result feed, removing row and falling back to network", e );
                    deleteCachedRepliesForActivity( activityId, db );
                }
                catch( ClassNotFoundException e ) {
                    Log.e( "exception when deserialising result feed, removing row and falling back to network", e );
                    deleteCachedRepliesForActivity( activityId, db );
                }
            }
            else if( failIfCacheOutdated ) {
                return null;
            }
        }
        finally {
            db.endTransaction();
        }

        return loadReplies( activityId );
    }

    private void deleteCachedRepliesForActivity( String activityId, SQLiteDatabase db ) {
        db.delete( StorageHelper.REPLIES_CACHE_TABLE, StorageHelper.REPLIES_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId } );
    }

    public BuzzReplyFeed loadReplies( String activityId ) throws IOException {
        long now = System.currentTimeMillis();

        DateHelper dateHelper = new DateHelper();

//        BuzzReplyFeed replyFeed = loadAndParse( BuzzUrl.buildCommentsUrl( null, activityId, null ), BuzzReplyFeed.class );
        BuzzReplyFeed replyFeed = loadFullReplyFeed( activityId );

        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            // Since SQLiteDatabase.insertWithOnConflict() doesn't exist in 2.1, we have to manually delete
            // the previous row here
            db.delete( StorageHelper.REPLIES_CACHE_TABLE,
                       StorageHelper.REPLIES_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId } );

            values.put( StorageHelper.REPLIES_CACHE_ACTIVITY_NAME, activityId );
            values.put( StorageHelper.REPLIES_CACHE_NUM_REPLIES, replyFeed.items == null ? 0 : replyFeed.items.size() );
            values.put( StorageHelper.REPLIES_CACHE_UPDATED_DATE, dateHelper.parseDate( replyFeed.updated ).getTime() );
            values.put( StorageHelper.REPLIES_CACHE_DOWNLOAD_DATE, now );
            values.put( StorageHelper.REPLIES_CACHE_CONTENT, SerialisationHelper.serialiseObject( replyFeed ) );
//        db.insertWithOnConflict( StorageHelper.REPLIES_CACHE_TABLE, StorageHelper.REPLIES_CACHE_CONTENT, values, SQLiteDatabase.CONFLICT_REPLACE );
            db.insert( StorageHelper.REPLIES_CACHE_TABLE, StorageHelper.REPLIES_CACHE_CONTENT, values );

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
        return replyFeed;
    }

    private BuzzReplyFeed loadFullReplyFeed( String activityId ) throws IOException {
        String baseUrl = BuzzUrl.buildCommentsUrl( null, activityId, null ) + "?max-results=50";
        Log.i( "loading full replies for: " + activityId );

        BuzzReplyFeed feed = loadAndParse( baseUrl, BuzzReplyFeed.class );

        BuzzReplyFeed currentFeed = feed;
        String next = currentFeed.findNextLink();
        if( next != null ) {
            // Ensure that the list is an ArrayList
            feed.items = new ArrayList<BuzzReply>( feed.items );

            while( next != null ) {
                currentFeed = loadAndParse( next, BuzzReplyFeed.class );
                if( currentFeed.items != null ) {
                    feed.items.addAll( currentFeed.items );
                }
                next = currentFeed.findNextLink();
            }
        }

        return feed;
    }

    public <T> T loadAndParse( String activityUrl, Class<T> feedType ) throws IOException {
        try {
            BuzzUrl url = new BuzzUrl( activityUrl );
            HttpRequest request = transport.buildGetRequest();
            request.url = url;
            HttpResponse result = request.execute();
            T feed = result.parseAs( feedType );
            return feed;
        }
        catch( HttpResponseException e ) {
            printResult( e.response );
            throw e;
        }
    }

    public BuzzActivity postActivity( BuzzActivity activity ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return null;
        }

        BuzzActivity result = postBuzzActivity( BuzzGlobal.ROOT_URL + "activities/@me/@self", activity, BuzzActivity.class );
        return result;
    }

    public BuzzReply postBuzzReply( String activityId, String content ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return null;
        }

        BuzzReply data = new BuzzReply();
        data.content = content;

        String activityUrl = BuzzUrl.buildCommentsUrl( null, activityId, null );
        BuzzReply result = postBuzzActivity( activityUrl, data, BuzzReply.class );

        loadReplies( activityId );
        context.sendBroadcast( IntentHelper.makeReplyFeedUpdatedIntent( activityId ) );

        return result;
    }

    public void editReply( String activityId, String replyId, String content ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return;
        }

        BuzzReply data = new BuzzReply();
        data.id = replyId;
        data.content = content;
        putBuzzActivity( BuzzUrl.buildCommentsUrl( null, activityId, replyId ), data, BuzzReply.class );

        loadReplies( activityId );
        context.sendBroadcast( IntentHelper.makeReplyFeedUpdatedIntent( activityId ) );
    }

    public void deleteReply( String activityId, String replyId ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return;
        }

        String url = BuzzUrl.buildCommentsUrl( null, activityId, replyId );
        deleteActivityByUrl( url );

        loadReplies( activityId );
        context.sendBroadcast( IntentHelper.makeReplyFeedUpdatedIntent( activityId ) );
    }

    public void editPost( String activityId, String content ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return;
        }

        BuzzActivity buzz = loadActivity( activityId, false );
        buzz.object.content = content;
        putBuzzActivity( BuzzUrl.buildActivityUrl( null, activityId ), buzz, BuzzActivity.class );

        loadActivity( activityId, true );
        context.sendBroadcast( IntentHelper.makeActivityUpdatedIntent( activityId ) );
    }

    public <T> T postBuzzActivity( String activityUrl, T data, Class<? extends T> feedType ) throws IOException {
        return sendRequest( activityUrl, data, feedType, transport.buildPostRequest() );
    }

    public <T> T putBuzzActivity( String activityUrl, T data, Class<? extends T> feedType ) throws IOException {
        return sendRequest( activityUrl, data, feedType, transport.buildPutRequest() );
    }

    private <T> T sendRequest( String url, T data, Class<? extends T> feedType, HttpRequest request ) throws IOException {
        Log.i( "sending request to: " + url );

        request.url = new BuzzUrl( url );
        JsonCContent content = new JsonCContent();
        content.data = data;
        request.content = content;
        return request.execute().parseAs( feedType );
    }

    public void putEmptyBuzzActivity( String activityUrl ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return;
        }

        HttpRequest request = transport.buildPutRequest();
        request.url = new BuzzUrl( activityUrl );
        InputStreamContent content = new InputStreamContent();
        content.setByteArrayInput( new byte[0] );
        content.type = "text/plain";
        request.content = content;
        request.execute();
    }

    public void deleteActivityByUrl( String url ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return;
        }

        HttpRequest request = transport.buildDeleteRequest();
        request.url = new BuzzUrl( url );
//        InputStreamContent content = new InputStreamContent();
//        content.setByteArrayInput( new byte[0] );
//        content.type = "text/plain";
//        request.content = content;
        request.execute();
    }

    public void updateLastReadDate( String activityId, long time ) {
        SQLiteDatabase db = getDatabase();
        ContentValues values = new ContentValues();
        values.put( StorageHelper.MESSAGE_CACHE_LAST_READ_DATE, time );
        db.update( StorageHelper.MESSAGE_CACHE_TABLE,
                   values,
                   StorageHelper.MESSAGE_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId } );
    }

    private synchronized SQLiteDatabase getDatabase() {
        if( db == null ) {
            db = StorageHelper.getDatabase( context );
        }
        return db.getDatabase();
    }

    public User loadProfile( String userId ) throws IOException {
        Log.d( "loading profile: " + userId );
        User user = loadAndParse( BuzzGlobal.ROOT_URL + "people/" + userId + "/@self", User.class );
        return user;
    }

    public UsersList searchUsers( String text, int itemsPerPage, int startIndex ) throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append( BuzzGlobal.ROOT_URL );
        buf.append( "people/search?q=" );
        buf.append( URLEncoder.encode( text, "UTF-8" ) );
        buf.append( "&max-results=" );
        buf.append( itemsPerPage );
        if( startIndex != -1 ) {
            buf.append( "&c=" );
            buf.append( startIndex );
        }
        UsersList users = loadAndParse( buf.toString(), UsersList.class );
        return users;
    }

    public UsersList searchUsersByTopic( String text, int itemsPerPage, int startIndex ) throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append( BuzzGlobal.ROOT_URL );
        buf.append( "activities/search/@people?q=" );
        buf.append( URLEncoder.encode( text, "UTF-8" ) );
        if( startIndex != -1 ) {
            buf.append( "&c=" );
            buf.append( startIndex );
        }
        UsersList users = loadAndParse( buf.toString(), UsersList.class );
        return users;
    }

    private void printResult( HttpResponse result ) {
        try {
            InputStream content = result.getContent();
            if( content == null ) {
                Log.i( "content is null" );
            }
            else {
                BufferedReader in = new BufferedReader( new InputStreamReader( content ) );
                String s;
                while( (s = in.readLine()) != null ) {
                    Log.i( "line:" + s );
                }
            }
        }
        catch( IOException e ) {
            throw new IllegalStateException( e );
        }
    }

    public List<String> loadFollowerIds() throws IOException {
        String url = BuzzGlobal.ROOT_URL + "people/@me/@groups/@followers?max-results=100";
        return loadFollowersOrFollowing( url );
    }

    public List<String> loadFollowingIds() throws IOException {
        String url = BuzzGlobal.ROOT_URL + "people/@me/@groups/@following?max-results=100";
        return loadFollowersOrFollowing( url );
    }

    public List<String> loadFollowersOrFollowing( String url ) throws IOException {
        List<String> ids = new ArrayList<String>();
        UsersList userList = loadUserListIdsFromUrl( ids, url );
        while( userList.entry != null && userList.entry.size() == userList.itemsPerPage ) {
            userList = loadUserListIdsFromUrl( ids, url + "&c=" + (userList.startIndex + userList.itemsPerPage) );
        }
        return ids;
    }

    private UsersList loadUserListIdsFromUrl( List<String> ids, String url ) throws IOException {
        UsersList usersList = loadAndParse( url, UsersList.class );
        if( usersList.entry != null ) {
            for( User user : usersList.entry ) {
                ids.add( user.id );
            }
        }
        return usersList;
    }

    public void startFollow( String userId ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return;
        }

        String url = BuzzGlobal.ROOT_URL + "people/@me/@groups/@following/" + userId;
        putEmptyBuzzActivity( url );

        SQLiteDatabase db = getDatabase();
        db.beginTransaction();
        try {
            // No support for SQLiteDatabase.insertWithOnConflict() in 2.1. Need to manually check
            // if the row already exists

            Cursor result = db.query( StorageHelper.FOLLOWING_TABLE,
                                      new String[] { StorageHelper.FOLLOWING_USER_ID },
                                      StorageHelper.FOLLOWING_USER_ID + " = ?", new String[] { userId },
                                      null, null,
                                      null );
            boolean hasRow = result.moveToNext();
            result.close();

            if( !hasRow ) {
                ContentValues values = new ContentValues();
                values.put( StorageHelper.FOLLOWING_USER_ID, userId );
                db.insert( StorageHelper.FOLLOWING_TABLE,
                           StorageHelper.FOLLOWING_USER_ID,
                           values );
            }

//        db.insertWithOnConflict( StorageHelper.FOLLOWING_TABLE,
//                                 StorageHelper.FOLLOWING_USER_ID,
//                                 values,
//                                 SQLiteDatabase.CONFLICT_IGNORE );

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    public void stopFollow( String userId ) throws IOException {
        if( DhsBuzzApp.BLOCK_DESTRUCTIVE_ACTIONS ) {
            return;
        }

        String url = BuzzGlobal.ROOT_URL + "people/@me/@groups/@following/" + userId;
        deleteActivityByUrl( url );

        SQLiteDatabase db = getDatabase();
        db.delete( StorageHelper.FOLLOWING_TABLE,
                   StorageHelper.FOLLOWING_USER_ID + " = ?", new String[] { userId } );
    }

    public GroupsResult loadGroups() throws IOException {
        String url = BuzzGlobal.ROOT_URL + "people/@me/@groups";
        return loadAndParse( url, GroupsResult.class );
    }

    public static class LoadActivitiesResult
    {
        public BuzzActivityFeed feed;
        public long lastRead;

        public LoadActivitiesResult( BuzzActivityFeed feed, long lastRead ) {
            this.feed = feed;
            this.lastRead = lastRead;
        }
    }
}
