package org.atari.dhs.buzztest.androidclient.tools;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class PendingIntentFactory
{
    private Intent intent;
    private boolean activity;

    public PendingIntentFactory( Intent intent, boolean activity ) {
        this.intent = intent;
        this.activity = activity;
    }

    public PendingIntent makePendingIntent( Context context, int requestCode ) {
        PendingIntent ret;
        if( activity ) {
            ret = PendingIntent.getActivity( context, requestCode, intent, 0 );
        }
        else {
            ret = PendingIntent.getService( context, requestCode, intent, 0 );
        }
        return ret;
    }
}
