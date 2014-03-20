package org.atari.dhs.buzztest.androidclient.c2dm;

import android.content.Context;
import android.content.Intent;

public interface C2DMHandler
{
    void processIncomingUpdate( Context context, Intent intent );
}
