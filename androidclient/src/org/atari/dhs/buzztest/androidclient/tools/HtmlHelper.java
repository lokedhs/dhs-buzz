package org.atari.dhs.buzztest.androidclient.tools;

public class HtmlHelper
{
    private HtmlHelper() {
        // prevent instantiation
    }

    public static void escapeHtmlChars( StringBuilder buf, String text ) {
        int n = text.length();
        int i = 0;
        while( i < n ) {
            int ch = text.codePointAt( i );
            if( ch > 127 ) {
                buf.append( "&#" );
                buf.append( String.valueOf( ch ) );
                buf.append( ";" );
            }
            else {
                buf.append( (char)ch );
            }
            i = text.offsetByCodePoints( i, 1 );
        }
    }
}
