package org.atari.dhs.buzztest.androidclient.displayprofile;

import java.text.MessageFormat;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.User;
import org.atari.dhs.buzztest.androidclient.displayfeed.DisplayFeed;
import org.atari.dhs.buzztest.androidclient.followers.FollowersManager;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.TabAsyncSupportActivity;
import org.atari.dhs.buzztest.androidclient.userlist.UserList;

public class DisplayProfileTabActivity extends TabAsyncSupportActivity implements LoadProfileTask.ProfileLoadable
{
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_PRELOADED_PROFILE = "userLoaded";

    private String userId;
    private User user;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent = getIntent();

        String localUserId = intent.getStringExtra( EXTRA_USER_ID );
        Preconditions.checkNotNull( localUserId );
        userId = localUserId;

        User user0 = (User)intent.getSerializableExtra( DisplayProfileTabActivity.EXTRA_PRELOADED_PROFILE );
        if( user0 == null ) {
            loadUser();
        }
        else {
            user = user0;
            createTabs();
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.menu_displayprofile, menu );
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        boolean enabled = user != null;
        menu.findItem( R.id.show_profile_url ).setEnabled( enabled );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        Log.i( "selected:" + item.getItemId() );
        switch( item.getItemId() ) {
            case R.id.show_profile_url:
                startActivity( new Intent( Intent.ACTION_VIEW, Uri.parse( user.profileUrl ) ) );
                return true;

            case R.id.display_messages:
                messagesButtonClicked();
                return true;

            case R.id.start_follow:
                startFollowButtonClicked();
                return true;

            case R.id.stop_follow:
                stopFollowButtonClicked();
                return true;

//            case R.id.followers:
//                followersButtonClicked( null );
//                return true;
//
//            case R.id.following:
//                followingButtonClicked( null );
//                return true;
        }

        return super.onOptionsItemSelected( item );
    }

    private void loadUser() {
        BuzzManager buzzManager = BuzzManager.createFromContext( this );
        FollowersManager followersManager = new FollowersManager( this );
        startAsyncTask( new LoadProfileTask(), new LoadProfileTask.Args( userId, false, true, buzzManager, followersManager ) );
    }

    @Override
    public void handleLoadResult( LoadProfileTask.Result result ) {
        if( result.errorMessage != null ) {
            Resources resources = getResources();
            String fmt = resources.getString( R.string.display_profile_load_error );
            Toast.makeText( this, MessageFormat.format( fmt, result.errorMessage ), Toast.LENGTH_LONG ).show();
        }
        else {
            user = result.user;
            createTabs();
        }
    }

    private void createTabs() {
        setTitle( user.displayName );

        TabHost tabHost = getTabHost();
        tabHost.addTab( createMainTabSpec( tabHost, user ) );
        tabHost.addTab( createFollowersTabSpec( tabHost, user ) );
        tabHost.addTab( createFollowingTabSpec( tabHost, user ) );
    }

    private TabHost.TabSpec createMainTabSpec( TabHost tabHost, User user ) {
        Intent newIntent = new Intent( this, DisplayProfile.class );
        newIntent.putExtra( EXTRA_USER_ID, user.id );
        newIntent.putExtra( EXTRA_PRELOADED_PROFILE, user );

        TabHost.TabSpec tabSpec = tabHost.newTabSpec( "main" );
        tabSpec.setIndicator( "Main" );
        tabSpec.setContent( newIntent );
        return tabSpec;
    }

    private TabHost.TabSpec createFollowersTabSpec( TabHost tabHost, User user ) {
        Intent intent = new Intent( this, UserList.class );
        intent.putExtra( UserList.EXTRA_TYPE, UserList.TYPE_FOLLOWERS );
        intent.putExtra( UserList.EXTRA_USER_ID, user.id );
        intent.putExtra( UserList.EXTRA_USER_NAME, user.displayName );

        TabHost.TabSpec tabSpec = tabHost.newTabSpec( "followers" );
        tabSpec.setIndicator( "Followers" );
        tabSpec.setContent( intent );

        return tabSpec;
    }

    private TabHost.TabSpec createFollowingTabSpec( TabHost tabHost, User user ) {
        Intent intent = new Intent( this, UserList.class );
        intent.putExtra( UserList.EXTRA_TYPE, UserList.TYPE_FOLLOWING );
        intent.putExtra( UserList.EXTRA_USER_ID, user.id );
        intent.putExtra( UserList.EXTRA_USER_NAME, user.displayName );

        TabHost.TabSpec tabSpec = tabHost.newTabSpec( "following" );
        tabSpec.setIndicator( "Following" );
        tabSpec.setContent( intent );

        return tabSpec;
    }

    public void messagesButtonClicked() {
        Intent intent = new Intent( this, DisplayFeed.class );
        intent.putExtra( DisplayFeed.EXTRA_FEED_TYPE, DisplayFeed.FEED_TYPE_NON_CACHED_DIRECT_FEED );
        intent.putExtra( DisplayFeed.EXTRA_FEED_URL, BuzzManager.makeUserIdFeedUrl( userId ) );
        intent.putExtra( DisplayFeed.EXTRA_FEED_TITLE, user.displayName );
        startActivity( intent );
    }

    public void startFollowButtonClicked() {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.updateFollow( userId, user.displayName, true );
            }
        } );
    }

    public void stopFollowButtonClicked() {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.updateFollow( userId, user.displayName, false );
            }
        } );
    }
}