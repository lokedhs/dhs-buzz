package org.atari.dhs.buzztest.androidclient.failedtasks;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceImpl;

public class FailedTasks extends Activity
{
    private SQLiteDatabaseWrapper db;
    private FailedTasksListAdapter failedTasksListAdapter;
    private LinearLayout listView;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.failed_tasks );

        db = StorageHelper.getDatabase( this );

        LayoutInflater inflater = (LayoutInflater)getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        listView = (LinearLayout)findViewById( R.id.error_list_view );
        failedTasksListAdapter = new FailedTasksListAdapter( this, inflater );
        failedTasksListAdapter.load( listView );
    }

    void restartTask( FailedTaskRow row ) {
        PostMessageServiceImpl.createIntentAndStart( this, row.command, row.bundle, false );
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    SQLiteDatabase getDatabase() {
        return db.getDatabase();
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void cancelAllClicked( View view ) {
        failedTasksListAdapter.removeAll( listView );
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void startAllClicked( View view ) {
        failedTasksListAdapter.restartAll( listView );
    }
}
