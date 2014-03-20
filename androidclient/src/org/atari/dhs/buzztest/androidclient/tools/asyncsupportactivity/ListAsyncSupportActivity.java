package org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity;

import android.app.ListActivity;
import android.os.Bundle;

public class ListAsyncSupportActivity extends ListActivity implements AsyncSupportActivityInterface
{
    private AsyncSupportActivityDelegate delegate;

    public ListAsyncSupportActivity() {
        delegate = new AsyncSupportActivityDelegate( this );
    }

    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        delegate.onCreate( savedInstanceState );
    }

    protected void onDestroy() {
        delegate.onDestroy();
        super.onDestroy();
    }

    public final Object onRetainNonConfigurationInstance() {
        return delegate.onRetainNonConfigurationInstance();
    }

    public Object getLastNonConfigurationInstance2() {
        return delegate.getLastNonConfigurationInstance2();
    }

    /**
     * When using <code>AsyncSupportActivity</code>, this method has to be overridden
     * instead of {@link #onRetainNonConfigurationInstance()}.
     *
     * @return the configuration state that should be saved
     */
    public Object onRetainNonConfigurationInstance2() {
        return null;
    }

    protected <A> void startAsyncTask( AsyncTaskWrapper<A, ?, ?> task, A ... args ) {
        delegate.startAsyncTask( task, args );
    }

    public void taskFinished( AsyncTaskWrapper<?, ?, ?> task ) {
        delegate.taskFinished( task );
    }
}
