package org.atari.dhs.buzztest.androidclient.displayfeed;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import android.app.SearchManager;
import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.Html;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import com.google.common.base.Joiner;

import org.atari.dhs.buzztest.androidclient.*;
import org.atari.dhs.buzztest.androidclient.buzz.*;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivityFeed;
import org.atari.dhs.buzztest.androidclient.displaybuzz.DisplayBuzz;
import org.atari.dhs.buzztest.androidclient.displayprofile.DisplayProfileTabActivity;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.post.PostActivity;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;
import org.atari.dhs.buzztest.androidclient.tools.*;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;
import org.atari.dhs.buzztest.androidclient.translation.TranslateActivity;

public class DisplayFeed extends AsyncSupportActivity
{
    public static final String EXTRA_FEED_TYPE = "feedType";

    public static final int FEED_TYPE_CONSUMPTION = 0;
    public static final int FEED_TYPE_NEARBY = 1;
    public static final int FEED_TYPE_NON_CACHED_DIRECT_FEED = 2;
    public static final int FEED_TYPE_SEARCH = 3;
    public static final int FEED_TYPE_REALTIME_SEARCH = 4;

    public static final String EXTRA_FEED_TITLE = "title";
    public static final String EXTRA_FEED_URL = "feedUrl";
    public static final String EXTRA_SEARCH_WORDS = "searchWords";
    public static final String EXTRA_REALTIME_SEARCH_NAME = "searchName";

    public static final String STORED_FEED_KEY_PERSONAL_FEED = "personalFeed";
    public static final String STORED_FEED_KEY_NEARBY = "nearby";

    private ImageCache imageCache;
    private BuzzManager buzzManager;
    private DisplayFeedListAdapter listAdapter;
    private SQLiteDatabaseWrapper dbWrapper;

    private BuzzFeedBroadcastReceiver recv = new BuzzFeedBroadcastReceiver();

    private TextView feedTitleTextView;
    private ImageButton toolbarSearchButton;
    private ImageButton toolbarFollowButton;
    private ImageButton toolbarNoFollowButton;

    /**
     * Timer used to periodically update the last reload status.
     */
    private Timer updateTimer;

    /**
     * Download date of the last feed that was downloaded.
     */
    private Long lastDownloadDate;

    /**
     * The activity url that should be displayed.
     */
    private String activityUrl;

    /**
     * The primary key used when looking up the stored feed in the database
     */
    private String storedFeedKey;

    /**
     * When the feed list is displaying a realtime feed, this value contains the ID of the feed.
     */
    private long realtimeSearchId = -1;

    /**
     * If the feed is a search feed, this member contains the list of search words, otherwise it's null.
     */
    private String[] searchWords;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent = getIntent();

        LayoutInflater inflater = (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        buzzManager = BuzzManager.createFromContext( this );
        imageCache = new ImageCache( this );

        setContentView( R.layout.message_list );

        feedTitleTextView = (TextView)findViewById( R.id.title_bar_text_view );
        toolbarSearchButton = (ImageButton)findViewById( R.id.search_toolbar_action );
        toolbarFollowButton = (ImageButton)findViewById( R.id.follow_toolbar_action );
        toolbarNoFollowButton = (ImageButton)findViewById( R.id.no_follow_toolbar_action );

        dbWrapper = StorageHelper.getDatabase( this );

        if( !setupTypeSpecificOptions( intent ) ) {
            finish();
            return;
        }

        ListView listView = (ListView)findViewById( R.id.message_list );
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                entryClicked( position );
            }
        } );

        Resources resources = getResources();
        int contactPhotoWidth = resources.getDimensionPixelSize( R.dimen.photo_icon_width );
        int contactPhotoHeight = resources.getDimensionPixelSize( R.dimen.photo_icon_height );

        listAdapter = new DisplayFeedListAdapter( this, inflater, imageCache, contactPhotoWidth, contactPhotoHeight );
        listView.setAdapter( listAdapter );

        toolbarSearchButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view ) {
                onSearchRequested();
            }
        } );

        registerForContextMenu( listView );
    }

    @Override
    protected void onDestroy() {
        imageCache.close();
        buzzManager.close();
        if( dbWrapper != null ) {
            dbWrapper.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if( lastDownloadDate == null ) {
            reloadContent( true, true );
        }
        else if( cacheIsUpdated() ) {
            Log.i( "reloading content. lastDownloadDate=" + (lastDownloadDate == null ? "NULL" : lastDownloadDate) );
            reloadContent( true, true );
        }

        IntentFilter filter = new IntentFilter( IntentHelper.ACTION_BUZZ_FEED_UPDATED );
        registerReceiver( recv, filter );
    }

    @Override
    protected void onStop() {
        unregisterReceiver( recv );
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateLastReloadMessage();

        if( realtimeSearchId != -1 ) {
            DhsBuzzApp app = (DhsBuzzApp)getApplication();
            app.setRealtimeActivityVisible( true );
        }

        updateTimer = new Timer();
        TimerTask task = new TimerTask()
        {
            @Override
            public void run() {
                updateLoadReloadOnMain();
            }
        };
        updateTimer.schedule( task, DateHelper.MINUTE_MILLIS, DateHelper.MINUTE_MILLIS );
    }

    @Override
    protected void onPause() {
        updateTimer.cancel();
        updateTimer = null;

        if( realtimeSearchId != -1 ) {
            DhsBuzzApp app = (DhsBuzzApp)getApplication();
            app.setRealtimeActivityVisible( false );
        }

        super.onPause();
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );

        FeedEntry activity = listAdapter.getActivityByPosition( ((AdapterView.AdapterContextMenuInfo)menuInfo).position );
        if( activity == null ) {
            // If the returned activity was null, then the selected row was the "load more" row.
            return;
        }
        final CachedFeedActivity buzz = activity.getActivity();

        String actorName = buzz.getActor();

        Resources resources = getResources();
        String format = resources.getString( R.string.display_buzz_menu_title_post );
        menu.setHeaderTitle( MessageFormat.format( format, actorName ) );

        final String actorId = buzz.getActorId();
        final String text = buzz.getTitle();

        menu.add( R.string.display_buzz_menu_start_profile ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ) {
                Intent intent = new Intent( DisplayFeed.this, DisplayProfileTabActivity.class );
                intent.putExtra( DisplayProfileTabActivity.EXTRA_USER_ID, actorId );
                startActivity( intent );
                return true;
            }
        } );

        menu.add( R.string.menu_text_translate ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ) {
                Intent intent = new Intent( DisplayFeed.this, TranslateActivity.class );
                intent.putExtra( TranslateActivity.EXTRA_TEXT, Html.fromHtml( text ).toString() );
                startActivity( intent );
                return true;
            }
        } );

        menu.add( R.string.display_buzz_menu_mute ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ) {
                muteFeed( buzz.getId() );
                return true;
            }
        } );
    }

    private void muteFeed( final String activityId ) {
        QueryDialogBox.DialogClickListener l = new QueryDialogBox.DialogClickListener()
        {
            @Override
            public void buttonPressed() {
                PostMessageServiceHelper.startServiceAndRunMethod( DisplayFeed.this, new AbstractServiceCallback()
                {
                    @Override
                    public void runWithService( IPostMessageService service ) throws RemoteException {
                        service.muteFeed( activityId );
                    }
                } );
            }
        };
        Resources resources = getResources();
        QueryDialogBox dialog = QueryDialogBox.create( this,
                                                       null, resources.getString( R.string.display_buzz_mute_message ),
                                                       resources.getString( R.string.display_buzz_mute_message_confirm ), l,
                                                       resources.getString( R.string.display_buzz_mute_message_cancel ), null
        );
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.menu_message_list, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.update_content:
                requestReloadUsingService();
                return true;
            case R.id.search:
                onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected( item );
        }
    }

    private boolean setupTypeSpecificOptions( Intent intent ) {
        if( Intent.ACTION_SEARCH.equals( intent.getAction() ) ) {
            String query = intent.getStringExtra( SearchManager.QUERY );
            Log.i( "got search query:\"" + query + "\"" );
            setupSearchOptions( TextUtils.split( query, " +" ) );
        }
        else {
            int type = intent.getIntExtra( EXTRA_FEED_TYPE, FEED_TYPE_CONSUMPTION );
            if( type == FEED_TYPE_CONSUMPTION ) {
                activityUrl = BuzzManager.getConsumptionFeedUrl();
                storedFeedKey = STORED_FEED_KEY_PERSONAL_FEED;
                feedTitleTextView.setText( R.string.display_feed_personal );
            }
            else if( type == FEED_TYPE_NEARBY ) {
                boolean wasSet = false;
                LocationManager locationManager = (LocationManager)getSystemService( LOCATION_SERVICE );
                String provider = PostActivity.findDecentLocationProvider( locationManager );
                if( provider != null ) {
                    Location lastLocation = locationManager.getLastKnownLocation( provider );
                    if( lastLocation != null ) {
                        double latitude = lastLocation.getLatitude();
                        double longitude = lastLocation.getLongitude();
                        int radius = 50000;

                        String locationSearchUrl = BuzzManager.makeLocationSearchUrl( latitude, longitude, radius, 0 );
                        activityUrl = locationSearchUrl;
                        storedFeedKey = STORED_FEED_KEY_NEARBY;
                        feedTitleTextView.setText( R.string.display_feed_nearby );
                        wasSet = true;
                    }
                }

                if( !wasSet ) {
                    Toast toast = Toast.makeText( this, R.string.display_feed_failed_to_get_location, Toast.LENGTH_LONG );
                    toast.show();
                    return false;
                }
            }
            else if( type == FEED_TYPE_NON_CACHED_DIRECT_FEED ) {
                String feedUrl = intent.getStringExtra( EXTRA_FEED_URL );
                if( feedUrl == null ) {
                    throw new IllegalStateException( "missing feed id" );
                }
                activityUrl = feedUrl;
                storedFeedKey = null;
                String feedTitle = intent.getStringExtra( EXTRA_FEED_TITLE );
                if( feedTitle == null ) {
                    throw new IllegalStateException( "missing feed title" );
                }
                feedTitleTextView.setText( feedTitle );
            }
            else if( type == FEED_TYPE_SEARCH ) {
                String[] searchWords = intent.getStringArrayExtra( EXTRA_SEARCH_WORDS );
                setupSearchOptions( searchWords );
            }
            else if( type == FEED_TYPE_REALTIME_SEARCH ) {
                if( !setupRealtimeSearchOptions( intent ) ) {
                    Toast.makeText( this, "No realtime search is currently active.", Toast.LENGTH_SHORT ).show();
                    return false;
                }
            }
            else {
                throw new IllegalStateException( "unknown feed type: " + type );
            }
        }

        return true;
    }

    private void setupSearchOptions( String[] searchWords ) {
        if( searchWords == null || searchWords.length == 0 ) {
            throw new IllegalStateException( "no search words specified" );
        }

        Log.i( "searching for: " + Arrays.toString( searchWords ) );

        this.searchWords = searchWords;
        activityUrl = BuzzUrl.buildSearchUrlFromParts( searchWords );
        storedFeedKey = null;
        feedTitleTextView.setText( "Search" );

        toolbarSearchButton.setVisibility( View.GONE );
        toolbarFollowButton.setVisibility( View.VISIBLE );
        toolbarNoFollowButton.setVisibility( View.GONE );
    }

    private boolean setupRealtimeSearchOptions( Intent intent ) {
        storedFeedKey = intent.getStringExtra( EXTRA_REALTIME_SEARCH_NAME );

        String url = null;
        SQLiteDatabase db = dbWrapper.getDatabase();
        db.beginTransaction();
        try {
            Cursor result = db.query( StorageHelper.REALTIME_SEARCH_TABLE,
                                      new String[] { StorageHelper.REALTIME_SEARCH_ID, StorageHelper.REALTIME_SEARCH_URL },
                                      StorageHelper.REALTIME_SEARCH_FEED_KEY + " = ?", new String[] { storedFeedKey },
                                      null, null,
                                      null );
            if( result.moveToNext() ) {
                realtimeSearchId = result.getLong( 0 );
                url = result.getString( 1 );
            }

            result.close();

            ContentValues values = new ContentValues();
            values.put( StorageHelper.REALTIME_SEARCH_NUM_UNREAD, 0 );
            db.update( StorageHelper.REALTIME_SEARCH_TABLE,
                       values,
                       StorageHelper.REALTIME_SEARCH_FEED_KEY + " = ?", new String[] { storedFeedKey } );

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }

        if( url == null ) {
            return false;
        }

        activityUrl = url;
        feedTitleTextView.setText( "Realtime" );

        toolbarSearchButton.setVisibility( View.GONE );
        toolbarFollowButton.setVisibility( View.GONE );
        toolbarNoFollowButton.setVisibility( View.VISIBLE );

        return true;
    }

    /**
     * Check if the cache content is newer than {@link #lastDownloadDate}.
     *
     * @return true if the cache content is newer than the content on the screen and thus needs to be reloaded
     */
    private boolean cacheIsUpdated() {
        if( storedFeedKey == null ) {
            return false;
        }

        SQLiteDatabase db = dbWrapper.getDatabase();
        Cursor result = db.query( StorageHelper.FEED_CACHE_TABLE,
                                  new String[] { StorageHelper.FEED_CACHE_DOWNLOAD_DATE },
                                  StorageHelper.FEED_CACHE_ID + " = ?", new String[] { storedFeedKey },
                                  null, null,
                                  null );

        long downloadDate = -1;
        if( result.moveToNext() ) {
            if( !result.isNull( 0 ) ) {
                downloadDate = result.getLong( 0 );
            }
        }

        result.close();

        Log.i( "downloadDate=" + downloadDate );
        return downloadDate != -1 && lastDownloadDate < downloadDate;

    }

    private void entryClicked( int position ) {
        if( listAdapter.hasNextLinkAndPositionIsNext( position ) ) {
            loadMore();
        }
        else {
            CachedFeedActivity buzz = listAdapter.getActivityByPosition( position ).getActivity();
            Intent intent = new Intent( this, DisplayBuzz.class );
            intent.putExtra( IntentHelper.EXTRA_ACTIVITY_ID, buzz.getId() );
            startActivity( intent );
        }
    }

    /**
     * Loads the feed and displays it in the ListView.
     *
     * @param prioritiseCache if true, then first try to load the feed from the cache instead of over the network
     * @param showProgress    if true, display a spinner while the load happens
     */
    private void reloadContent( final boolean prioritiseCache, boolean showProgress ) {
        Resources resources = getResources();
        ProgressDialogExecutor<LoadFeedResult> exec = new ProgressDialogExecutor<LoadFeedResult>( this, null, showProgress ? resources.getString( R.string.display_feed_loading ) : null );
        ProgressDialogExecutorTask<LoadFeedResult> task = new ProgressDialogExecutorTask<LoadFeedResult>()
        {
            @Override
            public LoadFeedResult run() throws BackgroundTaskException {
                Log.i( "reloadContent. prioCache=" + prioritiseCache );
                if( storedFeedKey == null ) {
                    return loadFeedByUrl();
                }
                else if( prioritiseCache ) {
                    PersistedFeedWrapper persistedFeed = buzzManager.loadFeedFromDatabase( storedFeedKey );
                    if( persistedFeed != null ) {
                        long lastReadDate = updateLastReadDate();
                        return new LoadFeedResult( persistedFeed, lastReadDate, null, persistedFeed.getDownloadDate() );
                    }
                }
                return null;
            }
        };
        ProgressDialogExecutorLoadCallback<LoadFeedResult> callback = new ProgressDialogExecutorLoadCallback<LoadFeedResult>()
        {
            @Override
            public void loadCompleted( LoadFeedResult result ) {
                if( result == null ) {
                    // the background load failed, reload from network
                    if( storedFeedKey == null ) {
                        // This is getting ridiculously messy.
                        //
                        // Essentially, we have three completely different ways of loading the feed:
                        //
                        //   1: Load from the cache (if storedFeedKey is non-null and there is data in the cache)
                        //   2: Load from a URL (if storedFeedKey is null)
                        //   3: Load using service (if there was nothing in the cache and we have a storedFeedKey)
                        //
                        // This code is indeed very inconsistent. In summary, the fact that stored feeds are loaded
                        // from the service is the core problem, and it should really be performed from this
                        // activity. The reason it still hasn't been moved is that we still might want to do
                        // background synchronisation of feeds.
                        //
                        // This code should not be made more messy until the service has been removed and the code
                        // made generic, and running inside this activity. For now, it will have to remain.
                        //
                        // At this point, the cache load has failed (result is null) and we thus have to load
                        // the feed using the service. In this situation we should have a storedFeedKey, so
                        // if we don't, there is some serious problem so we simply bail with an IllegalStateException.
                        throw new IllegalStateException( "result is null without a feed key" );
                    }
                    requestReloadUsingService();
                }
                else if( result.errorMessage != null ) {
                    displayErrorPopup( result.errorMessage );
                }
                else {
                    lastDownloadDate = result.feed.getDownloadDate();
                    listAdapter.setActivityFeed( result.feed.getFeed() );
                    updateLastReloadMessage();
                }
            }
        };
        exec.start( task, callback );
    }

    private long updateLastReadDate() {
        if( storedFeedKey != null ) {
            long now = System.currentTimeMillis();

            SQLiteDatabase db = dbWrapper.getDatabase();

            SQLiteDatabase db1 = dbWrapper.getDatabase();
            Cursor result = db1.query( StorageHelper.FEED_CACHE_TABLE,
                                       new String[] { StorageHelper.FEED_CACHE_LAST_READ_DATE },
                                       StorageHelper.FEED_CACHE_ID + " = ?", new String[] { storedFeedKey },
                                       null, null,
                                       null );

            long lastReadTime1 = -1;
            if( result.moveToNext() ) {
                if( !result.isNull( 0 ) ) {
                    lastReadTime1 = result.getLong( 0 );
                }
            }

            result.close();

            long lastReadTime = lastReadTime1;

            // No need to update if we never found a result
            if( lastReadTime != -1 ) {
                ContentValues values = new ContentValues();
                values.put( StorageHelper.FEED_CACHE_LAST_READ_DATE, now );
                db.update( StorageHelper.FEED_CACHE_TABLE,
                           values,
                           StorageHelper.FEED_CACHE_ID + " = ?", new String[] { storedFeedKey } );
            }

            return lastReadTime;
        }
        else {
            return -1;
        }
    }

    private void updateLoadReloadOnMain() {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run() {
                updateLastReloadMessage();
            }
        } );
    }

    private void updateLastReloadMessage() {
        if( lastDownloadDate != null ) {
            Resources resources = getResources();
            String fmt = resources.getString( R.string.display_feed_last_reload_date );
            setTitle( MessageFormat.format( fmt, DateHelper.makeDateDiffString( this, lastDownloadDate ) ) );
        }
    }

    private void requestReloadUsingService() {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.startSync( storedFeedKey, activityUrl );
            }
        } );
    }

    private LoadFeedResult loadFeedByUrl() {
        try {
            BuzzActivityFeed buzzFeed = buzzManager.loadAndParse( activityUrl, BuzzActivityFeed.class );
            PersistedFeedWrapper wrapper = new PersistedFeedWrapper( new CachedFeed( buzzFeed ), System.currentTimeMillis() );
            return new LoadFeedResult( wrapper, 0, null, 0 );
        }
        catch( IOException e ) {
            Log.w( "error loading feed", e );
            return new LoadFeedResult( null, 0, e.getLocalizedMessage(), 0 );
        }
    }

    private void loadMore() {
        final String nextLink = listAdapter.getNextLink();
        if( nextLink == null ) {
            throw new IllegalStateException( "trying to load next without a next link" );
        }

        if( storedFeedKey != null ) {
            PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
            {
                @Override
                public void runWithService( IPostMessageService service ) throws RemoteException {
                    service.loadMore( storedFeedKey, activityUrl );
                }
            } );
        }
        else {
            startAsyncTask( new LoadNextUnCachedTask(), new LoadNextUnCachedTask.Args( BuzzManager.createFromContext( this ), nextLink ) );
        }
    }

    void processLoadNextLinkResult( LoadNextUnCachedTask.Result result ) {
        if( result.errorMessage != null ) {
            displayErrorPopup( result.errorMessage );
        }
        else {
            listAdapter.addToEntries( result.feed.getMessages(), result.feed.getLoadNextLink() );
        }
    }

    private void displayErrorPopup( String errorMessage ) {
        Resources resources = getResources();
        String fmt = resources.getString( R.string.display_feed_error_feed_load );
        Toast.makeText( this, MessageFormat.format( fmt, errorMessage ), Toast.LENGTH_LONG ).show();
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void reloadButtonClicked( View view ) {
        requestReloadUsingService();
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void toolbarFollowButtonClicked( View view ) {
        // TODO: all searches now allowed
//        boolean found = false;
//        for( String s : searchWords ) {
//            if( s.length() >= 2 && s.charAt( 0 ) == '#' ) {
//                found = true;
//                break;
//            }
//        }

//        if( !found ) {
//            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder( this );
//            dialogBuilder.setMessage( "Realtime search can only be enabled for queries that contains at least one hashtag (a word beginning with the #-character)." );
//            dialogBuilder.setTitle( "Can\u2019t enable realtime" );
//            dialogBuilder.setPositiveButton( "OK", new DialogInterface.OnClickListener()
//            {
//                @Override
//                public void onClick( DialogInterface dialogInterface, int i ) {
//                    dialogInterface.dismiss();
//                }
//            } );
//            dialogBuilder.show();
//        }
//        else {
        QueryDialogBox.DialogClickListener okListener = new QueryDialogBox.DialogClickListener()
        {
            @Override
            public void buttonPressed() {
                startRealtime();
            }
        };
        QueryDialogBox dialog = QueryDialogBox.create( this, "Realtime Search", "Enable realtime updates for this search?",
                                                       "Enable", okListener,
                                                       "Cancel", null );
        dialog.show();
//        }
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void toolbarNoFollowButtonClicked( View view ) {
        stopRealtime();
        finish();
    }

    private void startRealtime() {
        Log.i( "starting realtime for words: " + Arrays.toString( searchWords ) );
        if( searchWords == null ) {
            throw new IllegalStateException( "it is only possible to perform a realtime subscription on search feeds" );
        }

        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                SQLiteDatabase db = dbWrapper.getDatabase();

                long now = System.currentTimeMillis();

                db.delete( StorageHelper.FEED_CACHE_TABLE,
                           StorageHelper.FEED_CACHE_ID + " = ?", new String[] { "search" } );
                CachedFeed cachedFeed = new CachedFeed();
                ContentValues feedCacheValues = new ContentValues();
                feedCacheValues.put( StorageHelper.FEED_CACHE_ID, "search" );
                feedCacheValues.put( StorageHelper.FEED_CACHE_CONTENT, SerialisationHelper.serialiseObject( cachedFeed ) );
                feedCacheValues.put( StorageHelper.FEED_CACHE_CREATED_DATE, now );
                feedCacheValues.put( StorageHelper.FEED_CACHE_DOWNLOAD_DATE, now );
                feedCacheValues.put( StorageHelper.FEED_CACHE_LAST_READ_DATE, 0 );
                db.insert( StorageHelper.FEED_CACHE_TABLE, StorageHelper.FEED_CACHE_CONTENT, feedCacheValues );

                service.startRealtimeFeedSubscription( Joiner.on( " " ).join( searchWords ) );
                finish();

                Intent intent = new Intent( DisplayFeed.this, DisplayFeed.class );
                intent.putExtra( EXTRA_FEED_TYPE, FEED_TYPE_REALTIME_SEARCH );
                intent.putExtra( EXTRA_REALTIME_SEARCH_NAME, "search" );
                startActivity( intent );
            }
        } );
    }

    private void stopRealtime() {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.stopRealtimeFeedSubscription( realtimeSearchId );
            }
        } );
    }

    private class BuzzFeedBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive( final Context context, final Intent intent ) {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run() {
                    String url = intent.getStringExtra( IntentHelper.EXTRA_FEED_ID );
                    if( activityUrl.equals( url ) ) {
                        reloadContent( true, false );
                    }
                }
            } );
        }
    }

    private static class LoadFeedResult
    {
        PersistedFeedWrapper feed;
        long lastReadDate;
        String errorMessage;
        long cachedFeedDownloadDate;

        private LoadFeedResult( PersistedFeedWrapper feed, long lastReadDate, String errorMessage, long cachedFeedDownloadDate ) {
            this.feed = feed;
            this.lastReadDate = lastReadDate;
            this.errorMessage = errorMessage;
            this.cachedFeedDownloadDate = cachedFeedDownloadDate;
        }
    }
}
