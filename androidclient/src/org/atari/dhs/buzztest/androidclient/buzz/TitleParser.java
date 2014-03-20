package org.atari.dhs.buzztest.androidclient.buzz;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleParser
{
    private Pattern pattern = Pattern.compile( "^Reshared post from[^\n]+(\n\n\n).*", Pattern.MULTILINE );

    public TitleParser() {
    }

    public String parseTitle( String s ) {
        Matcher matcher = pattern.matcher( s );
        if( !matcher.matches() ) {
//            System.out.println( "no match" );
            return s;
        }
        else {
            int start = matcher.start( 1 );
            int end = matcher.end( 1 );
//            System.out.printf( "start=%d, end=%d%n", start, end );
            StringBuilder buf = new StringBuilder();
            buf.append( s.substring( 0, start + 2 ) );
            buf.append( s.substring( end ) );
            return buf.toString();
        }
    }

    public static void main( String[] args ) {
        TitleParser p = new TitleParser();
        String s = "Reshared post from Kol Tregaskes\n\n\nIs there a web app that produces pop-up reminders for your Goo...";
        System.out.printf( "result = \"%s\"%n", p.parseTitle( s ) );
    }
}
