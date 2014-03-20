package org.atari.dhs.buzztest.androidclient.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import org.atari.dhs.buzztest.androidclient.Log;

public class PostMessageServiceHelper
{
    private PostMessageServiceHelper() {
        // prevent instantiation
    }

    public static void startServiceAndRunMethod( final Context context, final ServiceCallback callback ) {
        context.startService( new Intent( context, PostMessageService.class ) );
        ServiceConnection conn = new ServiceConnection()
        {
            @Override
            public void onServiceConnected( ComponentName componentName, IBinder iBinder ) {
                IPostMessageService service = IPostMessageService.Stub.asInterface( iBinder );
                try {
                    callback.runWithService( service );
                    context.unbindService( this );
                    callback.afterUnbind();
                }
                catch( RemoteException e ) {
                    Log.e( "got exception when starting sync", e );
                }
            }

            @Override
            public void onServiceDisconnected( ComponentName componentName ) {
            }
        };
        context.bindService( new Intent( context, PostMessageService.class ), conn, Context.BIND_AUTO_CREATE );
    }
}
