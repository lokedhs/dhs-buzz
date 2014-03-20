package org.atari.dhs.buzztest.androidclient.displaybuzz.editreply;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.atari.dhs.buzztest.androidclient.R;

public class EditReplyActivity extends Activity
{
    public static final String EXTRA_REPLY_TEXT = "reply";

    private Button postButton;
    private EditText editText;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.edit_reply );

        Intent intent = getIntent();
        String text = intent.getStringExtra( EXTRA_REPLY_TEXT );
        if( text == null ) {
            text = "";
        }

        postButton = (Button)findViewById( R.id.post_button );

        editText = (EditText)findViewById( R.id.content );
        editText.addTextChangedListener( new TextChangeListenerImpl() );
        editText.setText( text );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void postReplyClicked( View view ) {
        CharSequence text = editText.getText();
        if( text.length() == 0 ) {
            throw new IllegalStateException( "empty reply, can't save" );
        }

        Intent data = new Intent();
        data.putExtra( EXTRA_REPLY_TEXT, text );
        setResult( RESULT_OK, data );

        finish();
    }

    private class TextChangeListenerImpl implements TextWatcher
    {
        @Override
        public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
        }

        @Override
        public void onTextChanged( CharSequence s, int start, int before, int count ) {
        }

        @Override
        public void afterTextChanged( Editable editable ) {
            postButton.setEnabled( editable.length() > 0 );
        }
    }
}
