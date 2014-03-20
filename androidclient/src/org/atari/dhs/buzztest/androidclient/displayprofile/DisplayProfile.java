package org.atari.dhs.buzztest.androidclient.displayprofile;

import java.text.MessageFormat;

import android.app.ProgressDialog;
import android.content.*;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.User;
import org.atari.dhs.buzztest.androidclient.followers.FollowersManager;
import org.atari.dhs.buzztest.androidclient.imagecache.ImageCache;
import org.atari.dhs.buzztest.androidclient.imagecache.LoadImageCallback;
import org.atari.dhs.buzztest.androidclient.imagecache.StorageType;
import org.atari.dhs.buzztest.androidclient.tools.HtmlHelper;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;

public class DisplayProfile extends AsyncSupportActivity implements LoadProfileTask.ProfileLoadable
{
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_PRELOADED_PROFILE = "userLoaded";

    private String userId;
    private ImageCache imageCache;

    private User user;
    private ProgressDialog progressDialog;

    private boolean following;
    private boolean follower;

    private FollowingReceiver followingReceiver;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.display_profile );

        followingReceiver = new FollowingReceiver();

        Intent intent = getIntent();
        userId = intent.getStringExtra( EXTRA_USER_ID );
        if( userId == null ) {
            throw new IllegalStateException( "userId is null" );
        }

        imageCache = new ImageCache( this );

        loadUser( intent );
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter followingUpdatedFilter = new IntentFilter( IntentHelper.ACTION_FOLLOWING_UPDATED );
        registerReceiver( followingReceiver, followingUpdatedFilter );
    }

    @Override
    protected void onStop() {
        unregisterReceiver( followingReceiver );

        super.onStop();
    }

    private void loadUser( Intent intent ) {
        User userLoaded = (User)intent.getSerializableExtra( EXTRA_PRELOADED_PROFILE );
        BuzzManager buzzManager = BuzzManager.createFromContext( this );
        if( userLoaded == null ) {
            Resources resources = getResources();
            DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener()
            {
                @Override
                public void onCancel( DialogInterface dialogInterface ) {
                    finish();
                }
            };
            progressDialog = ProgressDialog.show( this, null, resources.getString( R.string.display_profile_loading_message ), true, true, cancelListener );
            startAsyncTask( new LoadProfileTask(), new LoadProfileTask.Args( userId, false, false, buzzManager, new FollowersManager( this ) ) );
        }
        else {
            this.user = userLoaded;
            populatePanel();
            startAsyncTask( new LoadProfileTask(), new LoadProfileTask.Args( userId, true, false, buzzManager, new FollowersManager( this ) ) );
        }
    }

    @Override
    public void handleLoadResult( LoadProfileTask.Result result ) {
        if( progressDialog != null ) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if( result.errorMessage != null ) {
            Resources resources = getResources();
            String fmt = resources.getString( R.string.display_profile_load_error );
            Toast.makeText( this, MessageFormat.format( fmt, result.errorMessage ), Toast.LENGTH_LONG ).show();
        }
        else {
            if( result.user != null ) {
                this.user = result.user;
                this.following = result.following;
                this.follower = result.follower;
                populatePanel();
            }
            else {
                this.following = result.following;
                this.follower = result.follower;
                updateFollowingTextField();
                updateFollowedByTextField();
            }
        }
    }

    private void populatePanel() {
        TextView nameTextView = (TextView)findViewById( R.id.name_text_view );
        nameTextView.setText( user.displayName );

        TextView interestsTextView = (TextView)findViewById( R.id.interests_text );
        if( user.interests != null && !user.interests.isEmpty() ) {
            interestsTextView.setVisibility( View.VISIBLE );
            SpannableStringBuilder buf = new SpannableStringBuilder();
            Resources resources = getResources();
            buf.append( resources.getString( R.string.display_profile_interests_title ) );
            buf.setSpan( new StyleSpan( Typeface.BOLD ), 0, buf.length(), 0 );
            buf.append( " " );
            boolean first = true;
            for( String s : user.interests ) {
                if( first ) {
                    first = false;
                }
                else {
                    buf.append( ", " );
                }
                buf.append( s );
            }
            interestsTextView.setText( buf );
        }
        else {
            interestsTextView.setText( "" );
            interestsTextView.setVisibility( View.GONE );
        }

        updateFollowingTextField();
        updateFollowedByTextField();

//        Button displayPostsButton = (Button)findViewById( R.id.messages_button );
//        String fmt = getResources().getString( R.string.display_profile_display_posts );
//        displayPostsButton.setText( MessageFormat.format( fmt, user.displayName ) );

        WebView aboutTextView = (WebView)findViewById( R.id.about_text_view );
        aboutTextView.setBackgroundColor( R.color.black );
        StringBuilder buf = new StringBuilder();
        buf.append( "<html><head>" );
        buf.append( "<style>" );
        buf.append( "body { background: #000000; color: #ffffff; } " );
        buf.append( "a { color: #a0a0ff }" );
        buf.append( "</style>" );
        buf.append( "</head>" );
        buf.append( "<body>" );
        if( user.aboutMe != null ) {
            HtmlHelper.escapeHtmlChars( buf, user.aboutMe );
        }
        buf.append( "</body>" );
        buf.append( "</html>" );
        aboutTextView.loadData( buf.toString(), "text/html", "utf-8" );

        final ImageView imageView = (ImageView)findViewById( R.id.image );
        if( user.thumbnailUrl == null ) {
            imageView.setImageResource( R.drawable.photo_default );
        }
        else {
            Resources resources = getResources();
            int contactPhotoWidth = resources.getDimensionPixelSize( R.dimen.photo_icon_width );
            int contactPhotoHeight = resources.getDimensionPixelSize( R.dimen.photo_icon_height );
            LoadImageCallback callback = new LoadImageCallback()
            {
                @Override
                public void bitmapLoaded( Bitmap bitmap ) {
                    imageView.setImageBitmap( bitmap );
                }
            };
            boolean background = imageCache.loadImage( user.thumbnailUrl, contactPhotoWidth, contactPhotoHeight, StorageType.LONG, callback );
            if( background ) {
                imageView.setImageResource( R.drawable.image_loading );
            }
        }
    }

    private void updateFollowingTextField() {
        updateTextViewForFollowingFollower( R.id.is_following_text, R.string.display_profile_user_following, following );
    }

    private void updateFollowedByTextField() {
        updateTextViewForFollowingFollower( R.id.is_followed_by_text, R.string.display_profile_user_followed_by, follower );
    }

    private void updateTextViewForFollowingFollower( int textViewResourceId, int messageResourceId, boolean active ) {
        TextView textView = (TextView)findViewById( textViewResourceId );
        if( active ) {
            textView.setVisibility( View.VISIBLE );

            Resources resources = getResources();
            String fmt = resources.getString( messageResourceId );

            textView.setText( MessageFormat.format( fmt, user.displayName ) );
        }
        else {
            textView.setVisibility( View.GONE );
        }
    }

    private class FollowingReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive( Context context, final Intent intent ) {
            runOnUiThread( new Runnable()
            {
                @Override
                public void run() {
                    String userId = intent.getStringExtra( IntentHelper.EXTRA_USER_ID );
                    boolean start = intent.getBooleanExtra( IntentHelper.EXTRA_FOLLOWING_TYPE_START, false );
                    if( userId.equals( userId ) ) {
                        following = start;
                        updateFollowingTextField();
                    }
                }
            } );
        }
    }
}
