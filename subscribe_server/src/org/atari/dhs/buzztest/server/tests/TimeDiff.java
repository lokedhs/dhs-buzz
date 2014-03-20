package org.atari.dhs.buzztest.server.tests;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeDiff
{
    public static final long SECOND_MILLIS = 1000;
    public static final long MINUTE_MILLIS = SECOND_MILLIS * 60;
    public static final long HOUR_MILLIS = MINUTE_MILLIS * 60;
    public static final long DAY_MILLIS = HOUR_MILLIS * 24;

    private static String makeDateDiffString( Date date ) {
        Calendar cal = Calendar.getInstance();

        long diffInMillis = System.currentTimeMillis() - date.getTime();
        
        if( diffInMillis <= MINUTE_MILLIS ) {
            return "now";
        }
        else if( diffInMillis <= HOUR_MILLIS ) {
            long mins = diffInMillis / MINUTE_MILLIS;
            return makePlural( mins, "minute", "minutes" );
        }
        else if( diffInMillis <= DAY_MILLIS ) {
            long hours = diffInMillis / HOUR_MILLIS;
            return makePlural( hours, "hour", "hours" );
        }
        else {
            long days = diffInMillis / DAY_MILLIS;
            return makePlural( days, "day", "days" );
        }
    }

    private static String makePlural( long n, String singular, String plural ) {
        if( n == 1 ) {
            return "one " + singular + " ago";
        }
        else {
            return n + " " + plural + " ago";
        }
    }

    public static void main( String[] args ) {
        try {
            DateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );

            String dateString = "2010-07-11T16:00:00";

            Date date = fmt.parse( dateString );
            System.out.println( "Diff: " + makeDateDiffString( date ) );
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }
}
