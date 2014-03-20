package org.atari.dhs.buzztest.androidclient;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StorageHelper extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 7;

    private static final String DATABASE_NAME = "dhsBuzzData";

    public static final String IMAGE_CACHE_TABLE = "imagecache";
    public static final String IMAGE_CACHE_NAME = "name";
    public static final String IMAGE_CACHE_FILENAME = "filename";
    public static final String IMAGE_CACHE_CREATED_DATE = "createdDate";
    public static final String IMAGE_CACHE_IMAGE_AVAILABLE = "imageAvailable";
    public static final String IMAGE_CACHE_CAN_DELETE = "canDelete";

    public static final String FEED_CACHE_TABLE = "feedcache";
    public static final String FEED_CACHE_ID = "feedId";
    public static final String FEED_CACHE_CREATED_DATE = "createdDate";
    public static final String FEED_CACHE_DOWNLOAD_DATE = "downloadDate";
    public static final String FEED_CACHE_CONTENT = "content";
    public static final String FEED_CACHE_LAST_READ_DATE = "lastRead";

    public static final String REPLIES_CACHE_TABLE = "repliesCache";
    public static final String REPLIES_CACHE_ACTIVITY_NAME = "activityName";
    public static final String REPLIES_CACHE_NUM_REPLIES = "numReplies";
    public static final String REPLIES_CACHE_UPDATED_DATE = "createdDate";
    public static final String REPLIES_CACHE_DOWNLOAD_DATE = "downloadDate";
    public static final String REPLIES_CACHE_CONTENT = "content";

    public static final String MESSAGE_CACHE_TABLE = "messageCache";
    public static final String MESSAGE_CACHE_ACTIVITY_NAME = "activityName";
    public static final String MESSAGE_CACHE_DOWNLOAD_DATE = "downloadDate";
    public static final String MESSAGE_CACHE_UPDATED = "updatedDate";
    public static final String MESSAGE_CACHE_TITLE = "title";
    public static final String MESSAGE_CACHE_CONTENT = "content";
    public static final String MESSAGE_CACHE_LAST_READ_DATE = "lastReadTime";

    public static final String UPDATE_COUNT_TABLE = "updatedLog";
    public static final String UPDATE_COUNT_ACTIVITY_NAME = "activityName";
    public static final String UPDATE_COUNT_NUMBER = "updateCount";
    public static final String UPDATE_COUNT_DATE = "updateCountRefreshDate";

    public static final String PENDING_TASK_TABLE = "pendingTasks";
    public static final String PENDING_TASK_ID = "id";
    public static final String PENDING_TASK_CREATED_DATE = "createdDate";
    public static final String PENDING_TASK_TITLE = "title";
    public static final String PENDING_TASK_MESSAGE = "message";
    public static final String PENDING_TASK_OBJECT = "content";

    public static final String REALTIME_USER_FEEDS_TABLE = "realtimeFeeds";
    public static final String REALTIME_USER_FEEDS_ID = "activityName";

    public static final String WATCHLIST_FEEDS_TABLE = "followingFeeds";
    public static final String WATCHLIST_FEEDS_ID = "activityName";
    public static final String WATCHLIST_FEEDS_CREATED_DATE = "createdDate";

    public static final String PURGE_STATISTICS_TABLE = "purgeDateLog";
    public static final String PURGE_STATISTICS_LAST_PURGE_DATE = "lastPurgeDate";

    public static final String FOLLOWERS_FOLLOWING_STATISTICS_TABLE = "followerStats";
    public static final String FOLLOWERS_FOLLOWING_STATISTICS_LAST_UPDATE = "lastUpdateDate";

    public static final String FOLLOWERS_TABLE = "followers";
    public static final String FOLLOWERS_USER_ID = "userId";

    public static final String FOLLOWING_TABLE = "following";
    public static final String FOLLOWING_USER_ID = "userId";

    public static final String REALTIME_SEARCH_TABLE = "realtimeSearchFeeds";
    public static final String REALTIME_SEARCH_ID = "id";
    public static final String REALTIME_SEARCH_FEED_KEY = "searchName";
    public static final String REALTIME_SEARCH_WORDS = "search";
    public static final String REALTIME_SEARCH_URL = "url";
    public static final String REALTIME_SEARCH_NUM_UNREAD = "numNew";

    StorageHelper( Context context ) {
        super( context, DATABASE_NAME, null, DATABASE_VERSION );
    }

    @Override
    public void onCreate( SQLiteDatabase db ) {
        db.execSQL( "create table " + IMAGE_CACHE_TABLE + " (" +
                    IMAGE_CACHE_NAME + " text primary key, " +
                    IMAGE_CACHE_FILENAME + " text, " +
                    IMAGE_CACHE_CREATED_DATE + " int not null, " +
                    IMAGE_CACHE_IMAGE_AVAILABLE + " boolean, " +
                    IMAGE_CACHE_CAN_DELETE + " boolean" +
                    ")" );

        db.execSQL( "create table " + FEED_CACHE_TABLE + " (" +
                    FEED_CACHE_ID + " text primary key, " +
                    FEED_CACHE_CREATED_DATE + " int not null, " +
                    FEED_CACHE_DOWNLOAD_DATE + " int not null, " +
                    FEED_CACHE_CONTENT + " blob not null, " +
                    FEED_CACHE_LAST_READ_DATE + " int" +
                    ")" );

        db.execSQL( "create table " + REALTIME_USER_FEEDS_TABLE + " (" +
                    REALTIME_USER_FEEDS_ID + " text primary key" +
                    ")" );

        db.execSQL( "create table " + PENDING_TASK_TABLE + " (" +
                    PENDING_TASK_ID + " integer primary key autoincrement, " +
                    PENDING_TASK_CREATED_DATE + " int not null, " +
                    PENDING_TASK_TITLE + " text not null, " +
                    PENDING_TASK_MESSAGE + " text not null, " +
                    PENDING_TASK_OBJECT + " blob not null" +
                    ")" );

        db.execSQL( "create table " + REPLIES_CACHE_TABLE + " (" +
                    REPLIES_CACHE_ACTIVITY_NAME + " text primary key, " +
                    REPLIES_CACHE_NUM_REPLIES + " int not null, " +
                    REPLIES_CACHE_UPDATED_DATE + " int not null, " +
                    REPLIES_CACHE_DOWNLOAD_DATE + " int not null, " +
                    REPLIES_CACHE_CONTENT + " blob not null" +
                    ")" );

        db.execSQL( "create table " + MESSAGE_CACHE_TABLE + " (" +
                    MESSAGE_CACHE_ACTIVITY_NAME + " text primary key, " +
                    MESSAGE_CACHE_DOWNLOAD_DATE + " int not null, " +
                    MESSAGE_CACHE_UPDATED + " int not null, " +
                    MESSAGE_CACHE_TITLE + " text, " +
                    MESSAGE_CACHE_CONTENT + " text, " +
                    MESSAGE_CACHE_LAST_READ_DATE + " int" +
                    ")" );

        db.execSQL( "create table " + UPDATE_COUNT_TABLE + " (" +
                    UPDATE_COUNT_ACTIVITY_NAME + " text primary key, " +
                    UPDATE_COUNT_NUMBER + " int not null, " +
                    UPDATE_COUNT_DATE + " int not null" +
                    ")" );

        db.execSQL( "create table " + WATCHLIST_FEEDS_TABLE + " (" +
                    WATCHLIST_FEEDS_ID + " text primary key, " +
                    WATCHLIST_FEEDS_CREATED_DATE + " int not null" +
                    ")" );

        db.execSQL( "create table " + PURGE_STATISTICS_TABLE + " (" +
                    PURGE_STATISTICS_LAST_PURGE_DATE + " int not null" +
                    ")" );

        db.execSQL( "create table " + FOLLOWERS_FOLLOWING_STATISTICS_TABLE + " (" +
                    FOLLOWERS_FOLLOWING_STATISTICS_LAST_UPDATE + " int not null" +
                    ")" );

        db.execSQL( "create table " + FOLLOWERS_TABLE + " (" +
                    FOLLOWERS_USER_ID + " text primary key" +
                    ")" );

        db.execSQL( "create table " + FOLLOWING_TABLE + " (" +
                    FOLLOWING_USER_ID + " text primary key" +
                    ")" );

        createTablesV6( db );
    }

    private void createTablesV6( SQLiteDatabase db ) {
        db.execSQL( "create table " + REALTIME_SEARCH_TABLE + " (" +
                    REALTIME_SEARCH_ID + " integer primary key autoincrement, " +
                    REALTIME_SEARCH_WORDS + " text unique not null, " +
                    REALTIME_SEARCH_FEED_KEY + " text not null, " +
                    REALTIME_SEARCH_URL + " text not null, " +
                    REALTIME_SEARCH_NUM_UNREAD + " int not null" +
                    ")" );
    }

    @Override
    public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
        if( newVersion != DATABASE_VERSION ) {
            throw new RuntimeException( "newVersion = " + newVersion + ", should be " + DATABASE_VERSION );
        }
        if( oldVersion < 6 ) {
            createTablesV6( db );
        }
        if( oldVersion < 7 ) {
            // The feed cache format has been changed, so we need to clear the table
            db.delete( StorageHelper.FEED_CACHE_TABLE,
                       null, null );
        }
    }

    public static SQLiteDatabaseWrapper getDatabase( Context context ) {
        Context appContext = context.getApplicationContext();
        return ((DhsBuzzApp)appContext).getDatabase();
    }
}
