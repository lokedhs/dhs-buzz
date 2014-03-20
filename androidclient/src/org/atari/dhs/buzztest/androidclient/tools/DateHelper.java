package org.atari.dhs.buzztest.androidclient.tools;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.content.res.Resources;

import org.atari.dhs.buzztest.androidclient.R;

public class DateHelper
{
    public static final long SECOND_MILLIS = 1000;
    public static final long MINUTE_MILLIS = SECOND_MILLIS * 60;
    public static final long HOUR_MILLIS = MINUTE_MILLIS * 60;
    public static final long DAY_MILLIS = HOUR_MILLIS * 24;

    private DateFormat inputFormat;
    private DateFormat dateTimeOutputFormat;

    public DateHelper() {
    }

    public synchronized Date parseDate( String text ) {
        try {
            return getInputFormat().parse( text );
        }
        catch( ParseException e ) {
            throw new IllegalArgumentException( "illegal date format: " + text, e );
        }
    }

    public synchronized String formatDateTimeOutputFormat( Date date ) {
        return getDateTimeOutputFormat().format( date );
    }

    private DateFormat getInputFormat() {
        if( inputFormat == null ) {
            inputFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
            inputFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        }
        return inputFormat;
    }

    private DateFormat getDateTimeOutputFormat() {
        if( dateTimeOutputFormat == null ) {
            dateTimeOutputFormat = DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.SHORT );
        }
        return dateTimeOutputFormat;
    }

    public static String makeDateDiffString( Context context, long date ) {
        Resources resources = context.getResources();

        long diffInMillis = System.currentTimeMillis() - date;

        if( diffInMillis <= MINUTE_MILLIS ) {
            return resources.getString( R.string.dates_less_than_one_minute_ago );
        }
        else if( diffInMillis <= HOUR_MILLIS ) {
            long mins = diffInMillis / MINUTE_MILLIS;
            String fmt = resources.getString( R.string.date_number_minutes_ago );
            return MessageFormat.format( fmt, mins );
        }
        else if( diffInMillis <= DAY_MILLIS ) {
            long hours = diffInMillis / HOUR_MILLIS;
            String fmt = resources.getString( R.string.date_number_hours_ago );
            return MessageFormat.format( fmt, hours );
        }
        else {
            long days = diffInMillis / DAY_MILLIS;
            String fmt = resources.getString( R.string.date_number_days_ago );
            return MessageFormat.format( fmt, days );
        }
    }
}
