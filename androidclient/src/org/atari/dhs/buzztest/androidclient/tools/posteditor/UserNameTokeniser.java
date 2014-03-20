package org.atari.dhs.buzztest.androidclient.tools.posteditor;

import android.widget.MultiAutoCompleteTextView;

class UserNameTokeniser implements MultiAutoCompleteTextView.Tokenizer
{
    @Override
    public int findTokenStart( CharSequence text, int cursor ) {
        if( cursor == 0 ) {
            return cursor;
        }

        int w = Character.offsetByCodePoints( text, cursor, -1 );
        while( true ) {
            int codePoint = Character.codePointAt( text, w );
            if( codePoint == '@' && (w == 0 || Character.isSpaceChar( Character.codePointBefore( text, w ))) ) {
                return Character.offsetByCodePoints( text, w, 1 );
            }
            else if( !isTokenCharacter( codePoint ) ) {
                return cursor;
            }

            if( w == 0 ) {
                break;
            }

            w = Character.offsetByCodePoints( text, w, -1 );
        }

        return cursor;
    }

    private boolean isTokenCharacter( int codePoint ) {
        return Character.isLetterOrDigit( codePoint )
                || codePoint == '.'
                || codePoint == '_'
                || codePoint == '@';
    }

    @Override
    public int findTokenEnd( CharSequence text, int cursor ) {
        int w = cursor;
        while( w < text.length() ) {
            int ch = Character.codePointAt( text, w );
            if( Character.isSpaceChar( ch ) ) {
                break;
            }

            w = Character.offsetByCodePoints( text, w, 1 );
        }

        return w;
    }

    @Override
    public CharSequence terminateToken( CharSequence text ) {
        if( text.length() == 0 ) {
            return text;
        }
        else if( Character.isSpaceChar( Character.codePointAt( text, text.length() - 1 ) ) ) {
            return text;
        }
        else {
            return text + " ";
        }
    }
}
