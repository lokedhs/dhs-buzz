package org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

public class AsyncSupportActivityDelegate
{
    private AsyncSupportActivityInterface activity;
    private List<AsyncTaskWrapper<?, ?, ?>> runningActivities = new ArrayList<AsyncTaskWrapper<?, ?, ?>>();
    private Object lastConfigurationInstance;

    public <T extends Activity & AsyncSupportActivityInterface> AsyncSupportActivityDelegate( T activity ) {
        this.activity = activity;
    }

    protected void onCreate( Bundle savedInstanceState ) {
        ActivityState state = (ActivityState)(((Activity)activity).getLastNonConfigurationInstance());
        if( state != null ) {
            this.lastConfigurationInstance = state.state;
            this.runningActivities = state.runningActivities;

            for( AsyncTaskWrapper<?,?,?> task : runningActivities ) {
                task.setActivity( activity );
            }
        }
    }

    protected void onDestroy() {
        List<AsyncTaskWrapper<?,?,?>> activitiesCopy;
        synchronized( runningActivities ) {
            activitiesCopy = new ArrayList<AsyncTaskWrapper<?,?,?>>( runningActivities );
            runningActivities.clear();
        }
        for( AsyncTaskWrapper<?,?,?> task : activitiesCopy ) {
            task.setActivity( null );
            task.cancel( true );
        }
    }

    protected <A> void startAsyncTask( AsyncTaskWrapper<A, ?, ?> task, A ... args ) {
        task.setActivity( activity );
        synchronized( runningActivities ) {
            runningActivities.add( task );
        }
        task.execute( args );
    }

    public final Object onRetainNonConfigurationInstance() {
        for( AsyncTaskWrapper<?,?,?> task : runningActivities ) {
            task.setActivity( null );
        }
        return new ActivityState( runningActivities, onRetainNonConfigurationInstance2() );
    }

    public Object getLastNonConfigurationInstance2() {
        return lastConfigurationInstance;
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

    void taskFinished( AsyncTaskWrapper<?,?,?> task ) {
        synchronized( runningActivities ) {
            runningActivities.remove( task );
        }
    }

    private static class ActivityState
    {
        private List<AsyncTaskWrapper<?,?,?>> runningActivities;
        private Object state;

        private ActivityState( List<AsyncTaskWrapper<?, ?, ?>> runningActivities, Object state ) {
            this.runningActivities = runningActivities;
            this.state = state;
        }
    }
}
