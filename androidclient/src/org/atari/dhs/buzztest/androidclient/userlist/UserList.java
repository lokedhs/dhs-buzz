package org.atari.dhs.buzztest.androidclient.userlist;

import java.text.MessageFormat;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzGlobal;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.User;
import org.atari.dhs.buzztest.androidclient.displayprofile.DisplayProfileTabActivity;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;

public class UserList extends AsyncSupportActivity
{
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_USER_NAME = "userName";
    public static final String EXTRA_URL = "url";

    public static final String TYPE_FOLLOWERS = "followers";
    public static final String TYPE_FOLLOWING = "following";

    private UserListViewAdapter userListViewAdapter;
    private String type;
    private String userName;
    private TextView userListTitleTextView;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_NO_TITLE );

        setContentView( R.layout.user_list );

        Intent intent = getIntent();
        type = getAndCheckExtra( intent, EXTRA_TYPE );
        userName = intent.getStringExtra( EXTRA_USER_NAME );

        String url = resolveUrl();

        userListTitleTextView = (TextView)findViewById( R.id.user_list_title );

        LayoutInflater inflater = (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        ListView listView = (ListView)findViewById( R.id.user_list_view );
        userListViewAdapter = new UserListViewAdapter( inflater );
        listView.setAdapter( userListViewAdapter );
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> adapterView, View view, int position, long id ) {
                rowClicked( position );
            }
        } );

        startAsyncTask( new LoadUserListTask(), new LoadUserListTask.Args( url, BuzzManager.createFromContext( this ) ) );
    }

    private String resolveUrl() {
        Intent intent = getIntent();

        String title;
        String url;

        if( type.equals( TYPE_FOLLOWERS ) ) {
            title = makeTitle( userName, R.string.user_list_title_followers, R.string.user_list_title_followers_no_username );
            url = BuzzGlobal.ROOT_URL + "people/" + getAndCheckExtra( intent, EXTRA_USER_ID ) + "/@groups/@followers";
        }
        else if( type.equals( TYPE_FOLLOWING ) ) {
            title = makeTitle( userName, R.string.user_list_title_following, R.string.user_list_title_following_no_username );
            url = BuzzGlobal.ROOT_URL + "people/" + getAndCheckExtra( intent, EXTRA_USER_ID ) + "/@groups/@following";
        }
        else {
            throw new IllegalStateException( "unknown type: " + type );
        }

        setTitle( title );
        return url;
    }

    private String makeTitle( String userName, int resourceId, int fallbackResourceId ) {
        Resources resources = getResources();
        if( userName == null ) {
            return resources.getString( fallbackResourceId );
        }
        else {
            String fmt = resources.getString( resourceId );
            return MessageFormat.format( fmt, userName );
        }
    }

    private String getAndCheckExtra( Intent intent, String key ) {
        String value = intent.getStringExtra( key );
        if( value == null ) {
            throw new IllegalStateException( "extra field " + key + " is null" );
        }
        return value;
    }

    private void rowClicked( int position ) {
        Row row = userListViewAdapter.getRowByPosition( position );
        User user = row.user;
        Intent intent = new Intent( this, DisplayProfileTabActivity.class );
        intent.putExtra( DisplayProfileTabActivity.EXTRA_USER_ID, user.id );
        intent.putExtra( DisplayProfileTabActivity.EXTRA_PRELOADED_PROFILE, user );
        startActivity( intent );
    }

    public void processLoadResult( LoadUserListTask.Result result ) {
        Resources resources = getResources();
        if( result.errorMessage != null ) {
            String fmt = resources.getString( R.string.user_list_error_loading );
            Toast toast = Toast.makeText( this, MessageFormat.format( fmt, result.errorMessage ), Toast.LENGTH_LONG );
            toast.show();
        }
        else {
            String title;
            if( type.equals( TYPE_FOLLOWERS ) ) {
                title = formatHeader( userName,
                                      result.users.totalResults,
                                      R.string.user_list_user_followers,
                                      R.string.user_list_user_followers_plain );
            }
            else if( type.equals( TYPE_FOLLOWING ) ) {
                title = formatHeader( userName,
                                      result.users.totalResults,
                                      R.string.user_list_user_following,
                                      R.string.user_list_user_following_plain );
            }
            else {
                throw new IllegalStateException( "illegal type: " + type );
            }
            userListTitleTextView.setText( title );
            userListViewAdapter.setUsers( result.users.entry );
        }
    }

    private String formatHeader( String userName, int totalResults, int resourceId, int resourceIdNoUsername ) {
        Resources resources = getResources();
        if( userName == null ) {
            String fmt = resources.getString( resourceIdNoUsername );
            return MessageFormat.format( fmt, totalResults );
        }
        else {
            return MessageFormat.format( resources.getString( resourceId ), userName, totalResults );
        }
    }
}
