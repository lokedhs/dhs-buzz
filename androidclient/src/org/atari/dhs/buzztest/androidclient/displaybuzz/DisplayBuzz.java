package org.atari.dhs.buzztest.androidclient.displaybuzz;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Editable;
import android.text.Selection;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.atari.dhs.buzztest.androidclient.*;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Actor;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.Replies;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.reply.BuzzReplyFeed;
import org.atari.dhs.buzztest.androidclient.displaybuzz.editreply.EditReplyActivity;
import org.atari.dhs.buzztest.androidclient.displaybuzz.editreshared.EditReshared;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;
import org.atari.dhs.buzztest.androidclient.settings.AutoRealtimeOption;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.QueryDialogBox;
import org.atari.dhs.buzztest.androidclient.tools.SerialisationHelper;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;

public class DisplayBuzz extends AsyncSupportActivity
{
    private static final int REQUEST_CODE_EDIT_POST = 1;
    private static final int REQUEST_CODE_EDIT_REPLY = 2;

    private BuzzManager buzzManager;

    /**
     * Set to <code>true</code> while the replies are being loaded.
     */
    private boolean loadingInProgress = false;

    private String activityId;
    private BuzzActivity buzz;

    private EditText replyTextView;
    private DisplayBuzzListAdapter contentAdapter;
    private ListView contentListView;

    private Pattern profileUrlPattern;
    private ImageCache imageCache;

    private BuzzActivityUpdatedBroadcastReceiver buzzActivityUpdatedReceiver = new BuzzActivityUpdatedBroadcastReceiver();
    private RepliesUpdatedBroadcastReceiver repliesUpdatedReceiver = new RepliesUpdatedBroadcastReceiver();
    private RealtimeChangedReceiver realtimeChangedReceiver = new RealtimeChangedReceiver();
    private PreferencesManager prefs;

    private SQLiteDatabaseWrapper dbWrapper;
    private long lastReadTime;
    private ProgressDialog progressDialog;

    private boolean isRealtimeSet;
    private boolean isRealtime;
    private String cachedUserId;

    /**
     * The id of the reply that is being edited while EditReplyActivity is being run.
     */
    private String editedReplyId;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.display_buzz );

        dbWrapper = StorageHelper.getDatabase( this );

        imageCache = new ImageCache( this );

        // At least one non-numeric character in the name part of the profile URL
//        profileUrlPattern = Pattern.compile( "^http://www.google.com/profiles/([a-zA-Z0-9_.-]*[a-zA-Z_.-]+[a-zA-Z0-9_.-]*)$" );
        profileUrlPattern = Pattern.compile( "^https://profiles.google.com/([a-zA-Z0-9_.-]*[a-zA-Z_.-]+[a-zA-Z0-9_.-]*)$" );

        buzzManager = BuzzManager.createFromContext( this );

        prefs = new PreferencesManager( this );

        Resources resources = getResources();
        int contactPhotoWidth = resources.getDimensionPixelSize( R.dimen.photo_icon_width );
        int contactPhotoHeight = resources.getDimensionPixelSize( R.dimen.photo_icon_height );

        Intent intent = getIntent();
        activityId = intent.getStringExtra( IntentHelper.EXTRA_ACTIVITY_ID );
        if( activityId == null ) {
            throw new IllegalStateException( "missing activity in intent" );
        }

        contentAdapter = new DisplayBuzzListAdapter( this, imageCache, contactPhotoWidth, contactPhotoHeight );
        contentListView = (ListView)findViewById( R.id.content_list_view );
        contentListView.setItemsCanFocus( true );
        contentListView.setAdapter( contentAdapter );
        contentListView.setClickable( true );
        contentListView.setOnItemClickListener( new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick( AdapterView<?> adapterView, View view, int position, long id ) {
                contentAdapter.handleItemClick( position );
            }
        } );
        registerForContextMenu( contentListView );

        replyTextView = (EditText)findViewById( R.id.reply );

        RuntimeConf lastConf = (RuntimeConf)getLastNonConfigurationInstance2();
        if( lastConf != null ) {
            loadingInProgress = lastConf.loadingInProgress;
            handleBuzzLoaded( lastConf.buzz, lastConf.lastReadTime, false );
            contentAdapter.setAdapterConf( lastConf.adapterConf );
        }
        else {
            boolean repliesForceReload = intent.getBooleanExtra( IntentHelper.EXTRA_REPLIES_FORCE_RELOAD, false );
            loadBuzzFromCacheOrBackground( repliesForceReload );
        }

        contentListView.requestFocus();
    }

    private void loadBuzzFromCacheOrBackground( boolean repliesForceReload ) {
        // This call will update the lastReadTime member
        CachedActivityInfo loadedBuzz = loadBuzzFromMessageCache( activityId );
        if( loadedBuzz != null ) {
            handleBuzzLoaded( loadedBuzz.getActivity(), loadedBuzz.getLastReadTime(), repliesForceReload );
        }
        else {
            if( progressDialog != null ) {
                throw new IllegalStateException( "progressDialog should be null" );
            }
            Resources resources = getResources();
            progressDialog = ProgressDialog.show( this, null, resources.getString( R.string.display_loading_progress_message ), true, true );
            startAsyncTask( new LoadBuzzTask(), new LoadBuzzTask.Args( activityId, buzzManager, repliesForceReload ) );
        }
    }

    void handleBuzzLoaded( BuzzActivity loadedBuzz, long lastReadTime, boolean repliesForceReload ) {
        this.buzz = loadedBuzz;
        this.lastReadTime = lastReadTime;
        contentAdapter.setBuzz( loadedBuzz, lastReadTime );

        Resources resources = getResources();
        String fmt = resources.getString( R.string.display_buzz_title );
        setTitle( MessageFormat.format( fmt, buzz.actor.name ) );
        loadRepliesUsingCacheStrategy( repliesForceReload ? RepliesLoadCacheStrategy.FORCE_NETWORK : RepliesLoadCacheStrategy.ONLY_IF_CACHED );
    }

    private CachedActivityInfo loadBuzzFromMessageCache( String activityId ) {
        long now = System.currentTimeMillis();

        SQLiteDatabase db = dbWrapper.getDatabase();
        Cursor result = db.query( StorageHelper.MESSAGE_CACHE_TABLE,
                                  new String[] { StorageHelper.MESSAGE_CACHE_LAST_READ_DATE, StorageHelper.MESSAGE_CACHE_CONTENT },
                                  StorageHelper.MESSAGE_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId },
                                  null, null,
                                  null );
        try {
            CachedActivityInfo info;

            if( !result.moveToNext() ) {
                info = null;
            }
            else {
                long lastReadTime = result.getLong( 0 );
                BuzzActivity activity = (BuzzActivity)SerialisationHelper.deserialiseObject( result.getBlob( 1 ) );
                result.close();

                ContentValues values = new ContentValues();
                values.put( StorageHelper.MESSAGE_CACHE_LAST_READ_DATE, now );
                db.update( StorageHelper.MESSAGE_CACHE_TABLE,
                           values,
                           StorageHelper.MESSAGE_CACHE_ACTIVITY_NAME + " = ?", new String[] { activityId } );

                info = new CachedActivityInfo( activity, lastReadTime );
            }
            result.close();
            return info;
        }
        catch( IOException e ) {
            Log.e( "failed to deserialise cached message", e );
            return null;
        }
        catch( ClassNotFoundException e ) {
            Log.e( "failed to deserialise cached message", e );
            return null;
        }
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );
        if( resultCode == RESULT_OK ) {
            switch( requestCode ) {
                case REQUEST_CODE_EDIT_POST:
                    updatePost( data.getCharSequenceExtra( EditReplyActivity.EXTRA_REPLY_TEXT ) );
                    break;

                case REQUEST_CODE_EDIT_REPLY:
                    updateReply( data.getCharSequenceExtra( EditReplyActivity.EXTRA_REPLY_TEXT ) );
                    break;
            }
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance2() {
        return new RuntimeConf( loadingInProgress, contentAdapter.getAdapterConf(), buzz, lastReadTime );
    }

    @Override
    protected void onDestroy() {
        imageCache.close();
        buzzManager.close();
        dbWrapper.close();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter activityUpdatedFilter = new IntentFilter( IntentHelper.ACTION_BUZZ_ACTIVITY_UPDATED );
        registerReceiver( buzzActivityUpdatedReceiver, activityUpdatedFilter );

        IntentFilter feedUpdatedFilter = new IntentFilter( IntentHelper.ACTION_REPLY_FEED_UPDATED );
        registerReceiver( repliesUpdatedReceiver, feedUpdatedFilter );

        IntentFilter realtimeChangedFilter = new IntentFilter( IntentHelper.ACTION_BUZZ_ACTIVITY_REALTIME_CHANGED );
        registerReceiver( realtimeChangedReceiver, realtimeChangedFilter );
    }

    @Override
    protected void onStop() {
        unregisterReceiver( buzzActivityUpdatedReceiver );
        unregisterReceiver( repliesUpdatedReceiver );
        unregisterReceiver( realtimeChangedReceiver );
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.menu_displaybuzz, menu );

        boolean hasC2DM = prefs.hasC2DM();
        menu.setGroupEnabled( R.id.realtime_menu_group, hasC2DM );
        if( hasC2DM ) {
            boolean isRealtime = checkIfRealtime();
            MenuItem addRealtime = menu.findItem( R.id.add_realtime );
            MenuItem removeRealtime = menu.findItem( R.id.remove_realtime );
//            addRealtime.setEnabled( !isRealtime );
//            removeRealtime.setEnabled( isRealtime );
        }

        return true;
    }

    boolean checkIfRealtime() {
        if( !isRealtimeSet ) {
            SQLiteDatabase db = dbWrapper.getDatabase();
            Cursor result = db.query( StorageHelper.REALTIME_USER_FEEDS_TABLE,
                                      new String[] { StorageHelper.REALTIME_USER_FEEDS_ID },
                                      StorageHelper.REALTIME_USER_FEEDS_ID + " = ?", new String[] { activityId },
                                      null, null,
                                      null );

            boolean v = result.moveToNext();
            result.close();

            isRealtime = v;
            isRealtimeSet = true;
        }
        return isRealtime;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        boolean result = super.onPrepareOptionsMenu( menu );

        boolean isRealtime = checkIfRealtime();
        MenuItem addRealtime = menu.findItem( R.id.add_realtime );
        MenuItem removeRealtime = menu.findItem( R.id.remove_realtime );
        if( prefs.hasC2DM() ) {
            addRealtime.setVisible( !isRealtime );
            removeRealtime.setVisible( isRealtime );
        }
        else {
            addRealtime.setVisible( false );
            removeRealtime.setVisible( false );
        }

        return result;
    }

    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.add_realtime:
                startRealtimeSubscription( true );
                return true;
            case R.id.remove_realtime:
                startRealtimeSubscription( false );
                return true;
            case R.id.mark_feed:
                addFeedToWatchlist();
                return true;
            case R.id.mark_as_liked:
                markAsLiked();
                return true;
            case R.id.mute:
                muteFeed();
                return true;
            case R.id.mark_as_spam:
                markAsSpam();
                return true;
            case R.id.reshare:
                reshareMessage();
                return true;
            default:
                return super.onMenuItemSelected( featureId, item );
        }
    }

    private void addFeedToWatchlist() {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.updateWatchlistStatus( buzz.id, true );
            }
        } );
    }

    private void startRealtimeSubscription( final boolean start ) {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.startRealtimeSubscription( buzz.id, buzz.actor.name, start );
            }
        } );
    }

    private void muteFeed() {
        QueryDialogBox.DialogClickListener l = new QueryDialogBox.DialogClickListener()
        {
            @Override
            public void buttonPressed() {
                PostMessageServiceHelper.startServiceAndRunMethod( DisplayBuzz.this, new AbstractServiceCallback()
                {
                    @Override
                    public void runWithService( IPostMessageService service ) throws RemoteException {
                        service.muteFeed( buzz.id );
                        finish();
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

    private void markAsSpam() {
        QueryDialogBox.DialogClickListener l = new QueryDialogBox.DialogClickListener()
        {
            @Override
            public void buttonPressed() {
                PostMessageServiceHelper.startServiceAndRunMethod( DisplayBuzz.this, new AbstractServiceCallback()
                {
                    @Override
                    public void runWithService( IPostMessageService service ) throws RemoteException {
                        service.markAsSpam( buzz.id );
                        finish();
                    }
                } );
            }
        };
        Resources resources = getResources();
        QueryDialogBox dialog = QueryDialogBox.create( this,
                                                       null, resources.getString( R.string.display_buzz_mark_spam ),
                                                       resources.getString( R.string.display_buzz_mark_spam_confirm ), l,
                                                       resources.getString( R.string.display_buzz_mark_spam_cancel ), null
        );
        dialog.show();
    }

    private void reshareMessage() {
        Intent intent = new Intent( this, EditReshared.class );
        intent.putExtra( EditReshared.EXTRA_ACTIVITY_ID, buzz.id );
        intent.putExtra( EditReshared.EXTRA_ORIGINAL_MESSAGE_SENDER, buzz.actor.name );
        startActivity( intent );
    }

    private void markAsLiked() {
        PostMessageServiceHelper.startServiceAndRunMethod( DisplayBuzz.this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.markAsLiked( buzz.id );
            }
        } );
    }

    void loadRepliesUsingCacheStrategy( RepliesLoadCacheStrategy mode ) {
        Replies replies = buzz.findReplies();
        if( replies != null && !loadingInProgress ) {
            startAsyncTask( new LoadRepliesTask(), new LoadRepliesTask.Args( buzz.id, replies, BuzzManager.createFromContext( this ), mode, System.currentTimeMillis() ) );
            loadingInProgress = true;
            contentAdapter.notifyReplyLoadStart();
        }
    }

    void handleReplyFeedResult( BuzzReplyFeed buzzReplyFeed, boolean displayErrorOnNull, String error ) {
        loadingInProgress = false;
        if( buzzReplyFeed != null ) {
            contentAdapter.setReplyFeed( buzzReplyFeed );
        }
        else {
            contentAdapter.resetLoadingMessage();
            if( displayErrorOnNull ) {
                Resources res = getResources();
                CharSequence message = res.getText( R.string.display_buzz_error_loading_replies );
                String formatted = MessageFormat.format( message.toString(), error == null ? "" : error );
                Toast toast = Toast.makeText( this, formatted, Toast.LENGTH_LONG );
                toast.show();
            }
        }
    }

    public boolean isLoadingInProgress() {
        return loadingInProgress;
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );
        if( v == contentListView ) {
            contentAdapter.handleContextMenu( menu, ((AdapterView.AdapterContextMenuInfo)menuInfo).position );
        }
    }

    public void addAtReply( Actor actor ) {
        String replyString = null;
        if( actor.profileUrl != null ) {
            Matcher matcher = profileUrlPattern.matcher( actor.profileUrl );
            if( matcher.matches() ) {
                replyString = "@" + matcher.group( 1 ) + "@gmail.com";
            }
        }
        if( replyString == null ) {
            replyString = "@" + actor.name;
        }

//        String fullReply = replyString + ", ";
//        replyTextView.setText( replyTextView.getText() + fullReply );
        Editable editableText = replyTextView.getEditableText();
        editableText.replace( Selection.getSelectionStart( editableText ),
                              Selection.getSelectionEnd( editableText ),
                              replyString );
//        Selection.setSelection( editableText, fullReply.length() );
        replyTextView.requestFocus();
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void postButtonClicked( View view ) {
        AutoRealtimeOption autoRealtime = prefs.getAutoRealtime();
        if( autoRealtime == AutoRealtimeOption.ASK ) {
            throw new UnsupportedOperationException( "realtime ask option is not implemented" );
        }

        final boolean enableRealtime = autoRealtime == AutoRealtimeOption.ENABLE && !checkIfRealtime();

        final String text = replyTextView.getText().toString();
        Replies replies = buzz.findReplies();
        if( text.length() > 0 && replies != null ) {
            final String replyFeed = replies.href;

            startService( new Intent( this, PostMessageService.class ) );
            ServiceConnection conn = new ServiceConnection()
            {
                @Override
                public void onServiceConnected( ComponentName componentName, IBinder iBinder ) {
                    IPostMessageService service = IPostMessageService.Stub.asInterface( iBinder );
                    try {
                        service.postReply( buzz.id, text, enableRealtime );
                        unbindService( this );
                    }
                    catch( RemoteException e ) {
                        Log.e( "got exception when trying to post message", e );
                    }
                }

                @Override
                public void onServiceDisconnected( ComponentName componentName ) {
                }
            };
            bindService( new Intent( this, PostMessageService.class ), conn, BIND_AUTO_CREATE );

            replyTextView.setText( "" );

            InputMethodManager imm = (InputMethodManager)getSystemService( Context.INPUT_METHOD_SERVICE );
            imm.hideSoftInputFromWindow( replyTextView.getApplicationWindowToken(), 0 );

            Toast toast = Toast.makeText( this, R.string.reply_posted_message, Toast.LENGTH_LONG );
            toast.show();
        }
    }

    String getUserId() {
        if( cachedUserId == null ) {
            cachedUserId = prefs.getUserId();
        }
        return cachedUserId;
    }

    void deleteReplySelected( final ReplyEntry replyEntry ) {
        QueryDialogBox.DialogClickListener deleteCallback = new QueryDialogBox.DialogClickListener()
        {
            @Override
            public void buttonPressed() {
                startDeleteReplyTask( replyEntry );
            }
        };
        Resources resources = getResources();
        QueryDialogBox dialog = QueryDialogBox.create( this,
                                                       resources.getString( R.string.display_buzz_delete_title ),
                                                       resources.getString( R.string.display_buzz_delete_content ),
                                                       resources.getString( R.string.display_buzz_delete_ok ), deleteCallback,
                                                       resources.getString( R.string.display_buzz_delete_keep ), null
        );
        dialog.show();
    }

    private void startDeleteReplyTask( final ReplyEntry replyEntry ) {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.deleteReply( buzz.id, replyEntry.getReply().id );
            }
        } );
    }

    void editPostSelected() {
        Intent intent = new Intent( this, EditReplyActivity.class );
        String content = buzz.object.content;
        intent.putExtra( EditReplyActivity.EXTRA_REPLY_TEXT, content );
        startActivityForResult( intent, REQUEST_CODE_EDIT_POST );
    }

    void editReplySelected( final ReplyEntry replyEntry ) {
        editedReplyId = replyEntry.getReply().id;

        Intent intent = new Intent( this, EditReplyActivity.class );
        String content = replyEntry.getReply().originalContent;
        if( content == null ) {
            content = "content missing";
        }
        intent.putExtra( EditReplyActivity.EXTRA_REPLY_TEXT, content );
        startActivityForResult( intent, REQUEST_CODE_EDIT_REPLY );
    }

    private void updatePost( CharSequence content ) {
        final String replyId = editedReplyId;
        editedReplyId = null;

        final String contentString = content.toString();

        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.editMessage( replyId, contentString );
            }
        } );
    }

    private void updateReply( CharSequence content ) {
        final String replyId = editedReplyId;
        editedReplyId = null;

        final String contentString = content.toString();

        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.editReply( buzz.id, replyId, contentString );
            }
        } );
    }

    void handleBackgroundLoadResult( LoadBuzzTask.LoadResult result ) {
        if( progressDialog != null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if( result.errorMessage != null ) {
            Resources resources = getResources();
            String fmt = resources.getString( R.string.display_buzz_error_loading_buzz );
            Toast toast = Toast.makeText( this, MessageFormat.format( fmt, result.errorMessage ), Toast.LENGTH_LONG );
            toast.show();
        }
        else {
            handleBuzzLoaded( result.buzz, 0, result.repliesForceReload );
        }
    }

    private static class RuntimeConf
    {
        boolean loadingInProgress;
        AdapterConf adapterConf;
        BuzzActivity buzz;
        long lastReadTime;

        private RuntimeConf( boolean loadingInProgress, AdapterConf adapterConf, BuzzActivity buzz, long lastReadTime ) {
            this.loadingInProgress = loadingInProgress;
            this.adapterConf = adapterConf;
            this.buzz = buzz;
            this.lastReadTime = lastReadTime;
        }
    }

    private class BuzzActivityUpdatedBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive( Context context, Intent intent ) {
            String activityId = intent.getStringExtra( IntentHelper.EXTRA_ACTIVITY_ID );
            if( activityId != null && activityId.equals( buzz.id ) ) {
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run() {
                        loadBuzzFromCacheOrBackground( false );
                    }
                } );
            }
        }
    }

    private class RepliesUpdatedBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive( Context context, Intent intent ) {
            String activityId = intent.getStringExtra( IntentHelper.EXTRA_ACTIVITY_ID );
            if( activityId != null && activityId.equals( buzz.id ) ) {
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run() {
                        loadRepliesUsingCacheStrategy( RepliesLoadCacheStrategy.PREFER_CACHE );
                    }
                } );
            }
        }
    }

    private class RealtimeChangedReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive( final Context context, final Intent intent ) {
            String activityId = intent.getStringExtra( IntentHelper.EXTRA_ACTIVITY_ID );
            if( activityId != null && activityId.equals( buzz.id ) ) {
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run() {
                        isRealtimeSet = true;
                        isRealtime = intent.getBooleanExtra( IntentHelper.EXTRA_REALTIME_MODE, false );
                        contentAdapter.updateRealtimeIcon( isRealtime );
                    }
                } );
            }
        }
    }
}
