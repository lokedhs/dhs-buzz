package org.atari.dhs.buzztest.androidclient.displaywatchlist;

import java.text.MessageFormat;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.CachedFeedActivity;
import org.atari.dhs.buzztest.androidclient.displaybuzz.DisplayBuzz;
import org.atari.dhs.buzztest.androidclient.displayfeed.DisplayFeedListAdapter;
import org.atari.dhs.buzztest.androidclient.displayfeed.FeedEntry;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;

public class DisplayWatchlist extends AsyncSupportActivity
{
    private ImageCache imageCache;
    private ProgressDialog progressDialog;
    private DisplayFeedListAdapter displayFeedListAdapter;
    private LoadWatchlistTask currentLoadingTask;
    private boolean reloadWhenFinished;
    private WatchlistUpdatedReceiver watchlistUpdatedReceiver;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.display_watchlist );

        LayoutInflater inflater = (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        ListView listView = (ListView)findViewById( R.id.list_view );
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                entryClicked( position );
            }
        } );

        watchlistUpdatedReceiver = new WatchlistUpdatedReceiver();

        Resources resources = getResources();
        int contactPhotoWidth = resources.getDimensionPixelSize( R.dimen.photo_icon_width );
        int contactPhotoHeight = resources.getDimensionPixelSize( R.dimen.photo_icon_height );

        imageCache = new ImageCache( this );
        displayFeedListAdapter = new DisplayFeedListAdapter( this, inflater, imageCache, contactPhotoWidth, contactPhotoHeight );
        listView.setAdapter( displayFeedListAdapter );
        registerForContextMenu( listView );

        startBackgroundLoadingTask( true );
    }

    private void startBackgroundLoadingTask( boolean displayProgressWindow ) {
        if( currentLoadingTask != null ) {
            reloadWhenFinished = true;
        }

        if( progressDialog != null ) {
            throw new IllegalStateException( "progress dialog should not be displayed at this time" );
        }

        if( displayProgressWindow ) {
            Resources resources = getResources();
            progressDialog = ProgressDialog.show( this, null, resources.getString( R.string.watchlist_loading ), true, true );
        }

        currentLoadingTask = new LoadWatchlistTask();
        startAsyncTask( currentLoadingTask, new LoadWatchlistTask.Args( StorageHelper.getDatabase( this ), BuzzManager.createFromContext( this ) ) );
    }

    @Override
    protected void onDestroy() {
        imageCache.close();
        super.onDestroy();
    }
    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter activityUpdatedFilter = new IntentFilter( IntentHelper.ACTION_WATCHLIST_UPDATED );
        registerReceiver( watchlistUpdatedReceiver, activityUpdatedFilter );
    }

    @Override
    protected void onStop() {
        unregisterReceiver( watchlistUpdatedReceiver );
        super.onStop();
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );

        FeedEntry activity = displayFeedListAdapter.getActivityByPosition( ((AdapterView.AdapterContextMenuInfo)menuInfo).position );
        CachedFeedActivity buzz = activity.getActivity();

        final String activityId = buzz.getId();
        String actorName = buzz.getActor();

        Resources resources = getResources();
        String format = resources.getString( R.string.display_buzz_menu_title_post );
        menu.setHeaderTitle( MessageFormat.format( format, actorName ) );

        menu.add( R.string.display_watchlist_menu_remove_from_watchlist ).setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick( MenuItem menuItem ) {
                removeFromWatchlist( activityId );
                return true;
            }
        } );
    }

    private void entryClicked( int position ) {
        CachedFeedActivity buzz = displayFeedListAdapter.getActivityByPosition( position ).getActivity();
        Intent intent = new Intent( this, DisplayBuzz.class );
        intent.putExtra( IntentHelper.EXTRA_ACTIVITY_ID, buzz.getId() );
        intent.putExtra( IntentHelper.EXTRA_REPLIES_FORCE_RELOAD, true );
        startActivity( intent );
    }

    private void removeFromWatchlist( final String activityId ) {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.updateWatchlistStatus( activityId, false );
            }
        } );
    }

    public void processActivitiesLoaded( LoadWatchlistTask.Result result ) {
        if( progressDialog != null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        boolean viewVisibility;
        if( result.errorMessage != null ) {
            Resources resources = getResources();
            String fmt = resources.getString( R.string.watchlist_error_load_messages );
            Toast.makeText( this, MessageFormat.format( fmt, result.errorMessage ), Toast.LENGTH_LONG ).show();
            viewVisibility = false;
        }
        else {
            displayFeedListAdapter.setStaticActivityList( result.activities );
            viewVisibility = result.activities.length > 0;
        }
        setViewVisibility( viewVisibility );

        if( reloadWhenFinished ) {
            reloadWhenFinished = false;
            startBackgroundLoadingTask( false );
        }
    }

    private void setViewVisibility( boolean resultVisible ) {
        View listView = findViewById( R.id.list_view );
        View noResultView = findViewById( R.id.no_result_view );
        listView.setVisibility( resultVisible ? View.VISIBLE : View.GONE );
        noResultView.setVisibility( resultVisible ? View.GONE : View.VISIBLE );
    }

    private class WatchlistUpdatedReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive( Context context, Intent intent ) {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run() {
                    startBackgroundLoadingTask( false );
                }
            } );
        }
    }
}
