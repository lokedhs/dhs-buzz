package org.atari.dhs.buzztest.androidclient;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import org.atari.dhs.buzztest.androidclient.debug.DebugInfoActivity;
import org.atari.dhs.buzztest.androidclient.displayfeed.DisplayFeed;
import org.atari.dhs.buzztest.androidclient.displaynew.DisplayNew;
import org.atari.dhs.buzztest.androidclient.displaywatchlist.DisplayWatchlist;
import org.atari.dhs.buzztest.androidclient.failedtasks.FailedTasks;
import org.atari.dhs.buzztest.androidclient.map.DhsBuzzMapActivity;
import org.atari.dhs.buzztest.androidclient.post.PostActivity;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.settings.Settings;
import org.atari.dhs.buzztest.androidclient.settings.StartActivityType;
import org.atari.dhs.buzztest.androidclient.userlist.SearchUserActivity;
import org.atari.dhs.buzztest.common.C2DMSettings;

public class WelcomeScreen extends Activity
{
    private View feedButton;
    private View nearbyButton;
    private View postButton;
    private View showWatchlistButton;
    private View searchUserButton;
    private View debugMapButton;
    private View noAccountInfoView;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.welcome_screen );

//        startPeriodicUpdates();

        feedButton = findViewById( R.id.feed_button );
        nearbyButton = findViewById( R.id.nearby_button );
        postButton = findViewById( R.id.post_button );
        showWatchlistButton = findViewById( R.id.watchlist_button );
        searchUserButton = findViewById( R.id.search_user_button );
        debugMapButton = findViewById( R.id.debug_map_button );
        noAccountInfoView = findViewById( R.id.no_account_wrapper_view );

        if( DemoExpiredActivity.demoExpired() ) {
            startActivity( new Intent( this, DemoExpiredActivity.class ) );
            finish();
            return;
        }

        String action = getIntent().getAction();
        if( action != null && action.equals( Intent.ACTION_MAIN ) ) {
            PreferencesManager prefs = new PreferencesManager( this );
            if( prefs.getBuzzSecret() != null ) {
                if( prefs.getLastVersionStarted() < DhsBuzzApp.getVersionCode( this ) ) {
                    startActivity( new Intent( this, NewsActivity.class ) );
                }
                else {
                    StartActivityType type = prefs.getDefaultStartActivity();
                    Log.i( "start activity type: " + type );
                    switch( type ) {
                        case WELCOME_SCREEN:
                            break;

                        case PERSONAL_FEED: {
                            Intent intent = new Intent( this, DisplayFeed.class );
                            intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_CONSUMPTION );
                            startAndCloseCurrentActivity( intent );
                            break;
                        }

                        case NEARBY: {
                            Intent intent = new Intent( this, DisplayFeed.class );
                            intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_NEARBY );
                            startAndCloseCurrentActivity( intent );
                            break;
                        }

                        default:
                            throw new IllegalStateException( "unknown default start activity: " + type );
                    }
                }
            }
        }
    }

    private void startAndCloseCurrentActivity( Intent intent ) {
        startActivity( intent );
//        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();

        PreferencesManager prefs = new PreferencesManager( this );
        boolean hasToken = prefs.getBuzzSecret() != null;

        feedButton.setEnabled( hasToken );
        nearbyButton.setEnabled( hasToken );
        postButton.setEnabled( hasToken );
        showWatchlistButton.setEnabled( hasToken );
        searchUserButton.setEnabled( hasToken );
        debugMapButton.setEnabled( hasToken );
        noAccountInfoView.setVisibility( hasToken ? View.GONE : View.VISIBLE );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.menu_welcome, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.settings:
                startActivity( new Intent( this, Settings.class ) );
                return true;

            case R.id.bookmarks:
                startActivity( new Intent( this, DisplayWatchlist.class ) );
                return true;

            case R.id.error_list:
                startActivity( new Intent( this, FailedTasks.class ) );
                return true;

            case R.id.debug_info:
                startActivity( new Intent( this, DebugInfoActivity.class ) );
                return true;

            case R.id.display_new:
                startActivity( new Intent( this, DisplayNew.class ) );
                return true;

            case R.id.beta_feedback_button:
                betaFeedbackClicked( null );
                return true;

            case R.id.search:
                onSearchRequested();
                return true;

            default:
                return super.onOptionsItemSelected( item );
        }
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void displayFeed( View view ) {
        Intent intent = new Intent( this, DisplayFeed.class );
        intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_CONSUMPTION );
        startActivity( intent );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void nearbyClicked( View view ) {
        Intent intent = new Intent( this, DisplayFeed.class );
        intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_NEARBY );
        startActivity( intent );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void watchlistClicked( View view ) {
        Intent intent = new Intent( this, DisplayFeed.class );
        intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_REALTIME_SEARCH );
        intent.putExtra( DisplayFeed.EXTRA_REALTIME_SEARCH_NAME, "search" );
        startActivity( intent );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void openSettings( View view ) {
        startActivity( new Intent( this, Settings.class ) );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void postNewMessage( View view ) {
        startActivity( new Intent( this, PostActivity.class ) );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void testRegisterC2DM( View view ) {
        Log.i( "registering C2DM" );
        Intent registrationIntent = new Intent( "com.google.android.c2dm.intent.REGISTER" );
        registrationIntent.putExtra( "app", PendingIntent.getBroadcast( this, 0, new Intent(), 0 ) ); // boilerplate
        registrationIntent.putExtra( "sender", C2DMSettings.C2DM_USER );
        startService( registrationIntent );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void testUnregisterC2DM( View view ) {
        Log.i( "unregistering C2DM" );
        Intent unregisterIntent = new Intent( "com.google.android.c2dm.intent.UNREGISTER" );
        unregisterIntent.putExtra( "app", PendingIntent.getBroadcast( this, 0, new Intent(), 0 ) );
        startService( unregisterIntent );
    }

    public void errorListClicked( View view ) {
        startActivity( new Intent( this, FailedTasks.class ) );
    }

    public void debugMapClicked( View view ) {
        startActivity( new Intent( this, DhsBuzzMapActivity.class ) );
    }

    public void searchUserClicked( View view ) {
        startActivity( new Intent( this, SearchUserActivity.class ) );
    }

    public void betaFeedbackClicked( View view ) {
        startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( "https://spreadsheets.google.com/viewform?formkey=dDRoNHdkM1dJWXBldzl6MUtoTjJPVHc6MQ" ) ) );
    }
}
