package org.atari.dhs.buzztest.androidclient.displaybuzz.loadhref;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.displaybuzz.DisplayBuzz;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncSupportActivity;

public class ReadHrefActivity extends AsyncSupportActivity
{
    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.readhref );

        Uri uri = getIntent().getData();
        startAsyncTask( new ReadHrefTask(), uri );
    }

    public void handleReadHrefResult( ReadHrefTask.Result result ) {
        if( result.errorMessage != null ) {
            Toast.makeText( this, result.errorMessage, Toast.LENGTH_SHORT ).show();
            finish();
        }
        else {
            Intent intent = new Intent( this, DisplayBuzz.class );
            intent.putExtra( IntentHelper.EXTRA_ACTIVITY_ID, result.activityId );
            startActivity( intent );
            finish();
        }
    }
}
