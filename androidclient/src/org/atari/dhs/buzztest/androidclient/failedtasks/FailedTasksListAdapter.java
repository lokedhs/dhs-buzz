package org.atari.dhs.buzztest.androidclient.failedtasks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.service.PersistedCommand;
import org.atari.dhs.buzztest.androidclient.tools.SerialisationHelper;

class FailedTasksListAdapter
{
    private FailedTasks parentActivity;
    private LayoutInflater inflater;

    private List<FailedTaskRow> rows;

    public FailedTasksListAdapter( FailedTasks parentActivity, LayoutInflater inflater ) {
        this.parentActivity = parentActivity;
        this.inflater = inflater;
    }

    void load( LinearLayout listView ) {
        SQLiteDatabase db = parentActivity.getDatabase();
        Cursor result = db.query( StorageHelper.PENDING_TASK_TABLE,
                                  new String[] { StorageHelper.PENDING_TASK_ID, StorageHelper.PENDING_TASK_TITLE, StorageHelper.PENDING_TASK_MESSAGE, StorageHelper.PENDING_TASK_OBJECT },
                                  null, null,
                                  null, null,
                                  StorageHelper.PENDING_TASK_CREATED_DATE );

        rows = new ArrayList<FailedTaskRow>();
        while( result.moveToNext() ) {
            try {
                long id = result.getLong( 0 );
                String title = result.getString( 1 );
                String message = result.getString( 2 );
                PersistedCommand cmd = (PersistedCommand)SerialisationHelper.deserialiseObject( result.getBlob( 3 ) );
                FailedTaskRow row = new FailedTaskRow( id, title, message, cmd.getCommand(), cmd.getArgs() );
                rows.add( row );
                createViewForRow( row, listView );
            }
            catch( ClassNotFoundException e ) {
                Log.e( "exception when retrieving failed task", e );
            }
            catch( IOException e ) {
                Log.e( "exception when retrieving failed task", e );
            }
        }
        result.close();
    }

    public void createViewForRow( final FailedTaskRow row, final LinearLayout listView ) {
        row.view = inflater.inflate( R.layout.failed_task_row, listView, false );

        TextView titleTextView = (TextView)row.view.findViewById( R.id.title );
        titleTextView.setText( row.title );

        TextView errorTextView = (TextView)row.view.findViewById( R.id.error_message );
        errorTextView.setText( row.message );

        Button restartButton = (Button)row.view.findViewById( R.id.resend_button );
        restartButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view ) {
                removeByRow( row, listView );
                parentActivity.restartTask( row );
            }
        } );

        Button cancelButton = (Button)row.view.findViewById( R.id.cancel_button );
        cancelButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick( View view ) {
                removeByRow( row, listView );
            }
        } );

        listView.addView( row.view );
    }

    private void removeByRow( FailedTaskRow row, LinearLayout listView ) {
        SQLiteDatabase db = parentActivity.getDatabase();
        deleteRowFromFailedTasksTable( row, db );

        rows.remove( row );
        listView.removeView( row.view );
    }

    public void removeAll( LinearLayout listView ) {
        SQLiteDatabase db = parentActivity.getDatabase();
        db.beginTransaction();
        try {
            for( FailedTaskRow row : rows ) {
                listView.removeView( row.view );
                deleteRowFromFailedTasksTable( row, db );
            }
            rows.clear();

            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    public void restartAll( LinearLayout listView ) {
        for( FailedTaskRow row : rows ) {
            listView.removeView( row.view );
            parentActivity.restartTask( row );
        }
        rows.clear();
    }

    private void deleteRowFromFailedTasksTable( FailedTaskRow row, SQLiteDatabase db ) {
        db.delete( StorageHelper.PENDING_TASK_TABLE, "id = ?", new String[] { String.valueOf( row.id ) } );
    }
}
