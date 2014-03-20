package org.atari.dhs.buzztest.androidclient.debug;

import java.util.Date;
import java.util.Formatter;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.service.AbstractServiceCallback;
import org.atari.dhs.buzztest.androidclient.service.IPostMessageService;
import org.atari.dhs.buzztest.androidclient.service.PostMessageServiceHelper;
import org.atari.dhs.buzztest.androidclient.service.task.PruneDatabaseTask;

public class DebugInfoActivity extends Activity
{
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.debug_info );

        SQLiteDatabaseWrapper dbWrapper = StorageHelper.getDatabase( this );
        try {
            SQLiteDatabase db = dbWrapper.getDatabase();

            TextView dbStatsTextView = (TextView)findViewById( R.id.debug_db_stats );
            dbStatsTextView.setText( createDbStats( db ) );
        }
        finally {
            dbWrapper.close();
        }
    }

    private CharSequence createDbStats( SQLiteDatabase db ) {
        StringBuilder buf = new StringBuilder();
        Formatter fmt = new Formatter( buf );

        countRows( db, StorageHelper.FEED_CACHE_TABLE, fmt );
        countRows( db, StorageHelper.IMAGE_CACHE_TABLE, fmt );
        countRows( db, StorageHelper.REALTIME_USER_FEEDS_TABLE, fmt );
        countRows( db, StorageHelper.MESSAGE_CACHE_TABLE, fmt );
        countRows( db, StorageHelper.PENDING_TASK_TABLE, fmt );
        countRows( db, StorageHelper.REPLIES_CACHE_TABLE, fmt );
        countRows( db, StorageHelper.UPDATE_COUNT_TABLE, fmt );

        long lastPurgeDate = PruneDatabaseTask.loadLastPurgeDate( db );
        fmt.format( "%nLast purge: %s%n", new Date( lastPurgeDate ).toString() );
        fmt.format( "Now: %s%n", new Date().toString() );

        return buf.toString();
    }

    private void countRows( SQLiteDatabase db, String table, Formatter fmt ) {
        Cursor result = db.rawQuery( "select count(*) from " + table, new String[0] );
        if( result.moveToNext() ) {
            fmt.format( "%s: %d%n", table, result.getInt( 0 ) );
        }
        else {
            fmt.format( "%s: error retrieving rows%n", table );
        }
        result.close();
    }

    @SuppressWarnings( { "UnusedDeclaration" })
    public void purgeDatabaseClicked( View view ) {
        PostMessageServiceHelper.startServiceAndRunMethod( this, new AbstractServiceCallback()
        {
            @Override
            public void runWithService( IPostMessageService service ) throws RemoteException {
                service.purge();
            }
        } );
    }
}
