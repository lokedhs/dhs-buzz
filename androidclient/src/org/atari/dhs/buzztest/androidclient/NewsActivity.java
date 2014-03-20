package org.atari.dhs.buzztest.androidclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.HtmlHelper;

public class NewsActivity extends Activity
{
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.news );

        PreferencesManager prefs = new PreferencesManager( this );
        int version = DhsBuzzApp.getVersionCode( this );
        if( prefs.getLastVersionStarted() < version ) {
            prefs.setLastVersionStarted( version );
        }

        WebView webView = (WebView)findViewById( R.id.content );
        AssetManager assetManager = getAssets();

        try {
            InputStream in = assetManager.open( "new_features.html" );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024 * 16];
            int n;
            while( (n = in.read( buf )) != -1 ) {
                out.write( buf, 0, n );
            }

            String s = new String( out.toByteArray(), "UTF-8" );
            StringBuilder builder = new StringBuilder();
            HtmlHelper.escapeHtmlChars( builder, s );
            String s2 = builder.toString();
            webView.loadData( s2, "text/html", "utf-8" );
        }
        catch( IOException e ) {
            Log.e( "error loading html data", e );
            finish();
        }
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void closeButtonClicked( View view ) {
        finish();
    }
}
