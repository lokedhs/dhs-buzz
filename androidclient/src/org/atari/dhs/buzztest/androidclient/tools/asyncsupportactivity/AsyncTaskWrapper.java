package org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity;

import android.app.Activity;
import android.os.AsyncTask;

public abstract class AsyncTaskWrapper<A, P, R> extends AsyncTask<A, P, R>
{
    private AsyncSupportActivityInterface underlyingActivity;
    private Object lock = new Object();

    void setActivity( AsyncSupportActivityInterface activity ) {
        synchronized( lock ) {
            this.underlyingActivity = activity;
            lock.notifyAll();
        }
    }

    @Override
    protected void onPostExecute( R r ) {
        super.onPostExecute( r );
        if( underlyingActivity != null ) {
            underlyingActivity.taskFinished( this );
        }
        onClose();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        onClose();
    }

    protected void onClose() {
    }

    protected Activity getUnderlyingActivity() {
        synchronized( lock ) {
            if( underlyingActivity == null ) {
                throw new IllegalStateException( "underlying activity is null, if this ever happens AsyncSupportActivity needs to be improved" );
            }
            return (Activity)underlyingActivity;
        }
    }
}
