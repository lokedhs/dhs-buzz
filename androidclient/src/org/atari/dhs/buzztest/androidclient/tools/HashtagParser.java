package org.atari.dhs.buzztest.androidclient.tools;

import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;

public class HashtagParser
{
    private HashtagParser() {
        // prevent instantiation
    }

    public static Spannable parseHashtaggedHtml( String htmlContent ) {
        Spanned parsed1 = Html.fromHtml( htmlContent );
        SpannableString parsed = new SpannableString( parsed1 );

        int len = parsed.length();
        int i = 0;
        while( i < len ) {
            char ch = parsed.charAt( i );
            if( ch == '#' ) {
                int start = i;
                int end = start + 1;
                while( end < len ) {
                    int wordCodePoint;
                    char wordChar = parsed.charAt( end );
                    if( Character.isHighSurrogate( wordChar ) ) {
                        end++;
                        if( end >= len ) {
                            throw new IllegalStateException( "string end with high surrogate" );
                        }
                        char second = parsed.charAt( end );
                        if( !Character.isLowSurrogate( second ) ) {
                            throw new IllegalStateException( "high surrogate followed by something which is not a low surrogate" );
                        }
                        wordCodePoint = Character.toCodePoint( wordChar, second );
                    }
                    else {
                        wordCodePoint = wordChar;
                    }

                    if( !Character.isLetterOrDigit( wordCodePoint ) ) {
                        break;
                    }

                    end++;
                }

                if( end - start > 1 ) {
                    CharSequence hashtag = parsed.subSequence( start, end );
                    URLSpan[] spans = parsed.getSpans( start, end, URLSpan.class );

                    if( spans != null ) {
                        for( URLSpan o : spans ) {
                            parsed.removeSpan( o );
                        }
                    }

                    parsed.setSpan( new HashTagSpan( hashtag.toString() ), start, end, 0 );
                }

                i = end;
            }
            else {
                i++;
            }
        }

        return parsed;
    }
}
