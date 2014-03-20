package org.atari.dhs.buzztest.androidclient.userlist;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.User;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.UsersList;
import org.atari.dhs.buzztest.androidclient.displayprofile.DisplayProfileTabActivity;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;

public class SearchUserActivity extends AsyncSupportActivity
{
    private EditText searchText;
    private TextView searchResultTextView;
    private SearchResultListAdapter searchResultListAdapter;
    private Spinner typeSpinner;
    private ArrayAdapter<TypeWrapper> typeSpinnerAdapter;

    private SearchUserTask currentTask;
    private ImageCache imageCache;
    private String currentSearchText;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        setContentView( R.layout.search_user );

        StoredConfig conf = (StoredConfig)getLastNonConfigurationInstance2();
        if( conf == null ) {
            imageCache = new ImageCache( this );
        }
        else {
            imageCache = conf.imageCache;
            currentTask = conf.currentTask;
            currentSearchText = conf.currentSearchText;
        }

        searchText = (EditText)findViewById( R.id.search_text );

        initTypeSpinner();

        Button searchButton = (Button)findViewById( R.id.search_button );
        searchButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view ) {
                searchClicked();
            }
        } );

        searchResultTextView = (TextView)findViewById( R.id.user_list_result );

        ListView listView = (ListView)findViewById( R.id.user_list );
        LayoutInflater inflater = (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        Resources resources = getResources();
        int contactPhotoWidth = resources.getDimensionPixelSize( R.dimen.photo_icon_width );
        int contactPhotoHeight = resources.getDimensionPixelSize( R.dimen.photo_icon_height );

        searchResultListAdapter = new SearchResultListAdapter( inflater, imageCache, contactPhotoWidth, contactPhotoHeight );
        if( conf != null ) {
            searchResultListAdapter.setConfig( conf.adapterConfig );
        }
        listView.setAdapter( searchResultListAdapter );
        listView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> adapterView, View view, int position, long id ) {
                handleRowClick( position );
            }
        } );
    }

    @Override
    protected void onDestroy() {
        imageCache.close();
        super.onDestroy();
    }

    @Override
    public Object onRetainNonConfigurationInstance2() {
        StoredConfig conf = new StoredConfig();
        conf.imageCache = imageCache;
        conf.currentTask = currentTask;
        conf.currentSearchText = currentSearchText;
        conf.adapterConfig = searchResultListAdapter.getConfig();
        return conf;
    }

    private void initTypeSpinner() {
        Resources resources = getResources();

        typeSpinner = (Spinner)findViewById( R.id.type_selection );
        ArrayAdapter<TypeWrapper> adapter = new ArrayAdapter<TypeWrapper>( this, android.R.layout.simple_spinner_item );
        typeSpinnerAdapter = adapter;
        typeSpinnerAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );

        adapter.add( new TypeWrapper( UserSearchType.NAME, resources.getString( R.string.search_user_type_name ) ) );
        adapter.add( new TypeWrapper( UserSearchType.TOPIC, resources.getString( R.string.search_user_type_topic ) ) );

        typeSpinner.setAdapter( adapter );
    }

    private void searchClicked() {
        String text = searchText.getText().toString();
        text = text.trim();
        if( text.length() == 0 ) {
            return;
        }
        if( currentTask == null ) {
            InputMethodManager imm = (InputMethodManager)getSystemService( Context.INPUT_METHOD_SERVICE );
            imm.hideSoftInputFromWindow( searchText.getApplicationWindowToken(), 0 );

            currentSearchText = text;
            startLoadTask( -1 );
        }
        else {
            Toast.makeText( this, R.string.search_user_already_in_progress, Toast.LENGTH_SHORT ).show();
        }
    }

    private void startLoadTask( int startIndex ) {
        if( currentSearchText == null ) {
            throw new IllegalStateException( "trying to load user list with no search text" );
        }

        setProgressBarIndeterminateVisibility( true );
        currentTask = new SearchUserTask();
        TypeWrapper selectedSearchType = typeSpinnerAdapter.getItem( typeSpinner.getSelectedItemPosition() );
        startAsyncTask( currentTask, new SearchUserTask.Args( currentSearchText, startIndex, selectedSearchType.type, BuzzManager.createFromContext( this ) ) );
    }

    private void handleRowClick( int position ) {
        int numRows = searchResultListAdapter.getUserCount();
        if( position == numRows ) {
            startLoadTask( numRows );
        }
        else {
            User user = searchResultListAdapter.getRowForPosition( position );

            Intent intent = new Intent( this, DisplayProfileTabActivity.class );
            intent.putExtra( DisplayProfileTabActivity.EXTRA_USER_ID, user.id );
            intent.putExtra( DisplayProfileTabActivity.EXTRA_PRELOADED_PROFILE, user );

            startActivity( intent );
        }
    }

    void processIncomingSearchResult( SearchUserTask.Result result ) {
        setProgressBarIndeterminateVisibility( false );
        currentTask = null;

        Resources resources = getResources();
        if( result.errorMessage != null ) {
            String fmt = resources.getString( R.string.search_user_error_loading_users );
            Toast.makeText( this, MessageFormat.format( fmt, result.errorMessage ), Toast.LENGTH_LONG ).show();
        }
        else {
            UsersList users = result.userList;

            String fmt = resources.getString( R.string.search_user_result_description );
            searchResultTextView.setText( MessageFormat.format( fmt, users.totalResults ) );

            boolean lastPage;
            List<User> userList;
            if( users.entry == null ) {
                userList = new ArrayList<User>();
                lastPage = true;
            }
            else {
                lastPage = users.itemsPerPage > users.entry.size();
                userList = users.entry;
            }

            if( result.startIndex == -1 ) {
                searchResultListAdapter.setUsers( userList, lastPage );
            }
            else {
                searchResultListAdapter.addUsers( userList, lastPage );
            }
        }
    }

    private static class TypeWrapper
    {
        UserSearchType type;
        String title;

        private TypeWrapper( UserSearchType type, String title ) {
            this.type = type;
            this.title = title;
        }

        public String toString() {
            return title;
        }
    }

    private static class StoredConfig
    {
        private SearchUserTask currentTask;
        private ImageCache imageCache;
        private String currentSearchText;
        private SearchResultListAdapter.StoredConfig adapterConfig;
    }
}
