package org.atari.dhs.buzztest.androidclient.tools.posteditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.widget.MultiAutoCompleteTextView;

public class PostEditor extends MultiAutoCompleteTextView
{
    public PostEditor( Context context ) {
        super( context );
    }

    public PostEditor( Context context, AttributeSet attrs ) {
        super( context, attrs );
    }

    public PostEditor( Context context, AttributeSet attrs, int defStyle ) {
        super( context, attrs, defStyle );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        init();
    }

    private void init() {
//        addTextChangedListener( new TextWatcher()
//        {
//            @Override
//            public void beforeTextChanged( CharSequence charSequence, int i, int i1, int i2 ) {
//            }
//
//            @Override
//            public void onTextChanged( CharSequence charSequence, int i, int i1, int i2 ) {
//                updateCursorPosition();
//            }
//
//            @Override
//            public void afterTextChanged( Editable editable ) {
//            }
//        } );

        setAdapter( new UserNameSuggestAdapter( getContext() ) );
        setTokenizer( new UserNameTokeniser() );

        int inputType = getInputType();
        setInputType( inputType & ~EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE );
    }

//    @Override
//    protected void performFiltering( CharSequence text, int keyCode ) {
////        text = "altered text";
//        Log.i( "performFiltering. text=" + text + ", keyCode=" + keyCode );
//        if( text instanceof SearchText ) {
//            super.performFiltering( text, keyCode );
//        }
//    }

//    @Override
//    public boolean onKeyDown( int keyCode, KeyEvent event ) {
//        boolean ret = super.onKeyDown( keyCode, event );
//        updateCursorPosition();
//        return ret;
//    }

//    private void updateCursorPosition() {
//        Log.i( "cursor position: s=" + getSelectionStart() + ", e=" + getSelectionEnd() );
//
//        int selectionStart = getSelectionStart();
//        int selectionEnd = getSelectionEnd();
//        if( selectionStart == selectionEnd ) {
//            Editable text = getText();
//            if( text.length() > 0 ) {
//                performFiltering( new SearchText( "special text" ), 0 );
//            }
//        }
//    }
//
//    @Override
//    protected void onSelectionChanged( int selStart, int selEnd ) {
//        super.onSelectionChanged( selStart, selEnd );
//        updateCursorPosition();
//    }


    @Override
    public int getThreshold() {
        return 1;
    }
}
