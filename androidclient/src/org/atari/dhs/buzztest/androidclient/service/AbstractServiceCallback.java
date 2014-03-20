package org.atari.dhs.buzztest.androidclient.service;

import android.os.RemoteException;

public class AbstractServiceCallback implements ServiceCallback
{
    @Override
    public void runWithService( IPostMessageService service ) throws RemoteException {
    }

    @Override
    public void afterUnbind() {
    }
}
