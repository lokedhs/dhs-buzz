package org.atari.dhs.buzztest.androidclient.settings.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.auth.oauth.*;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetAccessToken;
import com.google.api.client.googleapis.auth.oauth.GoogleOAuthGetTemporaryToken;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzGlobal;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.users.User;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.settings.Settings;
import org.atari.dhs.buzztest.androidclient.tools.BackgroundTaskException;
import org.atari.dhs.buzztest.androidclient.tools.ProgressDialogExecutor;
import org.atari.dhs.buzztest.androidclient.tools.ProgressDialogExecutorLoadCallback;
import org.atari.dhs.buzztest.androidclient.tools.ProgressDialogExecutorTask;

public class Authenticate extends Activity
{
    private static final String APPLICATION_DISPLAY_NAME = "DHS Buzz";
    private static final String CALLBACK_URI_SCHEME = "dhsbuzz-auth";

    private static OAuthCredentialsResponse credentials;
    private static boolean credentialsIsTemporaryFlag;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent = getIntent();
        Log.i( "onCreate. intent=" + intent );
        if( CALLBACK_URI_SCHEME.equals( intent.getScheme() ) && credentials != null && credentialsIsTemporaryFlag ) {
            setContentView( R.layout.authentication_info );
            handleAuthorisationResponse( intent );
        }
        else {//if( credentials == null ) {
            setContentView( R.layout.authenticate );

            TextView authenticationTextView = (TextView)findViewById( R.id.authentication_text_field );
            authenticationTextView.setMovementMethod( LinkMovementMethod.getInstance() );
            Resources resources = getResources();
            String authenticationInfo = resources.getString( R.string.authenticate_require_authentication );
            authenticationTextView.setText( Html.fromHtml( authenticationInfo ) );
        }
    }

    /**
     * Called when the authentication button is pressed.
     *
     * @param view the new that triggered the click
     */
    @SuppressWarnings( { "UnusedDeclaration" })
    public void requestAuthClicked( View view ) {
        authenticate();
    }

    @Override
    protected void onRestoreInstanceState( Bundle savedInstanceState ) {
        super.onRestoreInstanceState( savedInstanceState );
        Log.i( "onRestoreInstanceState" );
    }

    @Override
    protected void onNewIntent( Intent intent ) {
        Log.i( "got onNewIntent: " + intent );
        super.onNewIntent( intent );
    }

    @SuppressWarnings( { "MismatchedQueryAndUpdateOfCollection" })
    private void handleAuthorisationResponse( Intent intent ) {
        final Uri data = intent.getData();
        final String verifier = data.getQueryParameter( "oauth_verifier" );
        final String token = data.getQueryParameter( "oauth_token" );

        Log.i( "handleResponse. verifier=" + verifier + ", token=" + token );

        Resources resources = getResources();
        ProgressDialogExecutor<OAuthCredentialsResponse> exec = new ProgressDialogExecutor<OAuthCredentialsResponse>( this, null, resources.getString( R.string.auth_requesting_access ) );
        final ProgressDialogExecutorTask<OAuthCredentialsResponse> task = new ProgressDialogExecutorTask<OAuthCredentialsResponse>()
        {
            @Override
            public OAuthCredentialsResponse run() throws BackgroundTaskException {
                try {
                    OAuthCallbackUrl callbackUrl = new OAuthCallbackUrl( data.toString() );
                    GoogleOAuthGetAccessToken accessToken = new GoogleOAuthGetAccessToken();
                    accessToken.temporaryToken = callbackUrl.token;
                    accessToken.verifier = callbackUrl.verifier;
                    accessToken.signer = createOAuthSigner();
                    accessToken.consumerKey = "anonymous";

                    OAuthCredentialsResponse result = accessToken.execute();
                    updateLocalUserInformation( result );

                    return result;

                }
                catch( HttpResponseException e ) {
                    try {
                        throw new BackgroundTaskException( "Error from server: " + e.response.parseAsString(), e );
                    }
                    catch( IOException e2 ) {
                        throw new BackgroundTaskException( "IOException when reading HTTP result", e2 );
                    }
                }
                catch( IOException e ) {
                    throw new BackgroundTaskException( e );
                }
            }
        };
        ProgressDialogExecutorLoadCallback<OAuthCredentialsResponse> callback = new ProgressDialogExecutorLoadCallback<OAuthCredentialsResponse>()
        {
            @Override
            public void loadCompleted( OAuthCredentialsResponse result ) {
                Log.i( "Got permanent token: token=" + result.token + ", secret=" + result.tokenSecret );
                credentials = result;
                credentialsIsTemporaryFlag = false;

                Intent openSettingsIntent = new Intent( Authenticate.this, Settings.class );
                openSettingsIntent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
                openSettingsIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
                startActivity( openSettingsIntent );
            }
        };
        exec.start( task, callback );
    }

    private void updateLocalUserInformation( OAuthCredentialsResponse result ) throws IOException {
        BuzzManager buzzManager = BuzzManager.createFromKey( this, result.token, result.tokenSecret );
        User user = buzzManager.loadAndParse( BuzzGlobal.ROOT_URL + "people/@me/@self", User.class );

        PreferencesManager prefs = new PreferencesManager( this );
        prefs.setBuzzTokenAndSecret( result.token, result.tokenSecret, user.id );
    }

    private void authenticate() {
        Resources resources = getResources();
        ProgressDialogExecutor<OAuthCredentialsResponseResult> exec = new ProgressDialogExecutor<OAuthCredentialsResponseResult>( this, null, resources.getString( R.string.auth_request_authentication_with_service ) );
        ProgressDialogExecutorTask<OAuthCredentialsResponseResult> task = new ProgressDialogExecutorTask<OAuthCredentialsResponseResult>()
        {
            @Override
            public OAuthCredentialsResponseResult run() throws BackgroundTaskException {
                try {
                    Log.i( "starting auth" );
                    GoogleOAuthGetTemporaryToken temporaryToken = new GoogleOAuthGetTemporaryToken();
                    temporaryToken.signer = createOAuthSigner();
                    temporaryToken.consumerKey = "anonymous";
                    temporaryToken.scope = BuzzGlobal.OAUTH_SCOPE;
                    temporaryToken.displayName = APPLICATION_DISPLAY_NAME;
                    temporaryToken.callback = CALLBACK_URI_SCHEME + ":///";
                    Log.i( "executing token: " + temporaryToken );
                    OAuthCredentialsResponse result = temporaryToken.execute();
                    Log.i( "got result: " + result );
                    return new OAuthCredentialsResponseResult( result );
                }
                catch( HttpResponseException e ) {
                    HttpResponse response = e.response;
                    if( response == null ) {
                        throw new BackgroundTaskException( "response is null", e );
                    }
                    try {
                        BufferedReader in = new BufferedReader( new InputStreamReader( response.getContent() ) );
                        StringBuilder buf = new StringBuilder();
                        String s;
                        int maxBufLength = 512;
                        while( (s = in.readLine()) != null && buf.length() <= maxBufLength ) {
                            buf.append( s );
                            buf.append( "\n" );
                        }
                        in.close();
                        throw new BackgroundTaskException( "authentication error: " + buf.substring( 0, Math.min( buf.length(), maxBufLength ) ), e );
                    }
                    catch( IOException e1 ) {
                        throw new BackgroundTaskException( "got IOException while loading content from response: " + e1.getMessage(), e );
                    }
                }
                catch( IOException e ) {
                    Log.e( "got exception when requesting authentication", e );
                    return new OAuthCredentialsResponseResult( e.getLocalizedMessage() );
                }
            }
        };
        ProgressDialogExecutorLoadCallback<OAuthCredentialsResponseResult> callback = new ProgressDialogExecutorLoadCallback<OAuthCredentialsResponseResult>()
        {
            @Override
            public void loadCompleted( OAuthCredentialsResponseResult response ) {
                Log.i( "loadCompleted" );
                if( response.errorMessage != null ) {
                    String fmt = getResources().getString( R.string.authenticate_error );
                    Toast.makeText( Authenticate.this, MessageFormat.format( fmt, response.errorMessage ), Toast.LENGTH_LONG ).show();
                }
                else {
                    OAuthAuthorizeTemporaryTokenUrl authoriseUrl = new OAuthAuthorizeTemporaryTokenUrl( BuzzGlobal.OAUTH_AUTHORIZATION_URL );
                    authoriseUrl.set( "scope", BuzzGlobal.OAUTH_SCOPE );
                    authoriseUrl.set( "domain", "anonymous" );
                    authoriseUrl.set( "xoauth_displayname", APPLICATION_DISPLAY_NAME );
                    authoriseUrl.temporaryToken = response.response.token;
                    Intent webIntent = new Intent( Intent.ACTION_VIEW );
                    webIntent.setData( Uri.parse( authoriseUrl.build() ) );
                    webIntent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
                    webIntent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET );
    //                webIntent.addFlags( Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS );
    //                webIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );

                    credentials = response.response;
                    credentialsIsTemporaryFlag = true;

                    Log.i( "starting intent: " + webIntent );
                    startActivity( webIntent );

                    // Solution suggested from stackoverflow:
                    // http://stackoverflow.com/questions/3464045/opening-browser-activity-but-prevent-it-from-being-in-the-activity-history
    //                finish();
                }
            }
        };
        exec.start( task, callback );
    }

    private static OAuthHmacSigner createOAuthSigner() {
        OAuthHmacSigner result = new OAuthHmacSigner();
        if( credentials != null ) {
            result.tokenSharedSecret = credentials.tokenSecret;
        }
        result.clientSharedSecret = "anonymous";
        return result;
    }

    private static OAuthParameters createOAuthParameters() {
        OAuthParameters authoriser = new OAuthParameters();
        authoriser.consumerKey = "anonymous";
        authoriser.signer = createOAuthSigner();
        authoriser.token = credentials.token;
        return authoriser;
    }
}
