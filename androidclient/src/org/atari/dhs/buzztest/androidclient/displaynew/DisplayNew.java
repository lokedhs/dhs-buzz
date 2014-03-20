package org.atari.dhs.buzztest.androidclient.displaynew;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.atari.dhs.buzztest.androidclient.IntentHelper;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.StorageHelper;

public class DisplayNew extends Activity
{
    private SQLiteDatabaseWrapper db;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.display_new );

        db = StorageHelper.getDatabase( this );

        ListView listView = (ListView)findViewById( R.id.list_view );
        final DisplayNewListAdapter adapter = new DisplayNewListAdapter( this );
        if( adapter.getCount() == 1 ) {
            // If there is only a single new message, simply go directly to it
            DisplayNewRow row = adapter.getRowAndRemove( 0 );
            startActivity( IntentHelper.makeDisplayBuzzIntent( DisplayNew.this, row.activityId, true ) );
            // Finish, so the back button will not go back to the empty page
            finish();
        }
        else {
            listView.setAdapter( adapter );
            listView.setOnItemClickListener( new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick( AdapterView<?> adapterView, View view, int position, long id ) {
                    DisplayNewRow row = adapter.getRowAndRemove( position );
                    startActivity( IntentHelper.makeDisplayBuzzIntent( DisplayNew.this, row.activityId, true ) );
                }
            } );
        }
    }

    @Override
    protected void onDestroy() {
        db.close();
        super.onDestroy();
    }

    SQLiteDatabase getDatabase() {
        return db.getDatabase();
    }
}
