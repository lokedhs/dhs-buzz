package org.atari.dhs.buzztest.androidclient.displaybuzz.editreshared;

import java.text.MessageFormat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;

public class EditReshared extends Activity
{
    public static final String EXTRA_ACTIVITY_ID = "activityId";
    public static final String EXTRA_ORIGINAL_MESSAGE_SENDER = "actorName";

    private EditText contentTextField;

    private String activityId;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.reshare );

        TextView senderNameField = (TextView)findViewById( R.id.sender_name );
        contentTextField = (EditText)findViewById( R.id.content );

        Intent intent = getIntent();
        activityId = intent.getStringExtra( EXTRA_ACTIVITY_ID );
        if( activityId == null ) {
            throw new IllegalStateException( "activityId must not be null" );
        }

        String actorName = intent.getStringExtra( EXTRA_ORIGINAL_MESSAGE_SENDER );
        if( actorName == null ) {
            throw new IllegalStateException( "actorName must not be null" );
        }

        String fmt = getResources().getString( R.string.reshare_sender_name );
        senderNameField.setText( MessageFormat.format( fmt, actorName ) );
    }

    public void postButtonClicked( View view ) {
        String comment = contentTextField.getText().toString().trim();
        if( comment.equals( "" ) ) {
            comment = null;
        }
        final String comment0 = comment;

        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.postReshare( activityId, comment0 );
            }
        } );

        finish();
    }
}