package org.atari.dhs.buzztest.androidclient.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PostMessageService extends Service
{
    public static final String ACTION_HIDE_BUZZ_FROM_FEED = "org.atari.dhs.buzz.HIDE_BUZZ_FROM_FEED";
    public static final String EXTRA_BUZZ_ID = "buzzId";

    public static final String EXTRA_SERVICE_CALL = "callServiceTask";
    public static final String SERVICE_CALL_SYNC_PERSONAL_FEED = "syncFeed";

    public PostMessageService() {
        super();
    }

    @Override
    public IBinder onBind( Intent intent ) {
        return new PostMessageServiceImpl( this );
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart( Intent intent, int startId ) {
        super.onStart( intent, startId );
    }
}
