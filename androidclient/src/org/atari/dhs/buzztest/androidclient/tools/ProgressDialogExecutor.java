package org.atari.dhs.buzztest.androidclient.tools;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.atari.dhs.buzztest.androidclient.Log;

public class ProgressDialogExecutor<T>
{
    private Context context;
    private CharSequence title;
    private CharSequence message;

    public ProgressDialogExecutor( Context context, CharSequence title, CharSequence message )
    {
        this.context = context;
        this.title = title;
        this.message = message;
    }

    public void start( ProgressDialogExecutorTask<T> task, ProgressDialogExecutorLoadCallback<T> callback )
    {
        LoaderTask loaderTask = new LoaderTask( task, callback );
        if( message != null ) {
            ProgressDialog loadingDialog = ProgressDialog.show( context, title, message, true, false );
            loaderTask.setLoadingDialog( loadingDialog );
        }
        loaderTask.execute();
    }

    private class LoaderTask extends AsyncTask<Void, Void, T>
    {
        private ProgressDialogExecutorTask<T> task;
        private ProgressDialogExecutorLoadCallback<T> callback;
        private ProgressDialog loadingDialog;

        public LoaderTask( ProgressDialogExecutorTask<T> task, ProgressDialogExecutorLoadCallback<T> callback )
        {
            this.task = task;
            this.callback = callback;
        }

        @Override
        protected T doInBackground( Void... args )
        {
            try {
                T result = task.run();
                return result;
            }
            catch( BackgroundTaskException e ) {
                Log.e( "Exception when processing task", e );
                throw new RuntimeException( e );
            }
        }

        @Override
        protected void onPostExecute( T result )
        {
            super.onPostExecute( result );

            if( loadingDialog != null ) {
                loadingDialog.dismiss();
                loadingDialog = null;
            }

            callback.loadCompleted( result );
        }

        public void setLoadingDialog( ProgressDialog loadingDialog )
        {
            this.loadingDialog = loadingDialog;
        }
    }
}
