package org.atari.dhs.buzztest.androidclient.service;

import android.os.RemoteException;

public interface ServiceCallback
{
    void runWithService( IPostMessageService service ) throws RemoteException;

    void afterUnbind();
}
