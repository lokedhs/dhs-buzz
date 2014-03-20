package org.atari.dhs.buzztest.androidclient.displaynew;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.StorageHelper;
import org.atari.dhs.buzztest.androidclient.buzz.TitleParser;

public class DisplayNewListAdapter extends BaseAdapter
{
    private DisplayNew parentActivity;
    private List<DisplayNewRow> rows;
    private LayoutInflater inflater;

    public DisplayNewListAdapter( DisplayNew parentActivity ) {
        this.parentActivity = parentActivity;

        this.inflater = (LayoutInflater)parentActivity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        loadEntriesFromDatabase();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem( int position ) {
        return rows.get( position );
    }

    @Override
    public long getItemId( int position ) {
        return position;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        DisplayNewRow row = rows.get( position );

        View v;
        if( convertView != null && convertView.getId() == R.id.display_new_row_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.display_new_row, parent, false );
        }

        TextView titleTextView = (TextView)v.findViewById( R.id.title );
        if( row.title == null ) {
            titleTextView.setText( "unknown" );
        }
        else {
            titleTextView.setText( row.title );
        }

        TextView unreadCountTextView = (TextView)v.findViewById( R.id.unread_count );
        unreadCountTextView.setText( String.valueOf( row.updateCount ) );

        return v;
    }

    private void loadEntriesFromDatabase() {
        SQLiteDatabase db = parentActivity.getDatabase();
//        Cursor result = db.query( StorageHelper.MESSAGE_CACHE_TABLE,
//                                  new String[] { StorageHelper.MESSAGE_CACHE_ACTIVITY_NAME,
//                                                 StorageHelper.MESSAGE_CACHE_TITLE,
//                                                 StorageHelper.MESSAGE_CACHE_UPDATE_COUNT,
//                                                 StorageHelper.MESSAGE_CACHE_UPDATE_COUNT_DATE },
//                                  StorageHelper.MESSAGE_CACHE_UPDATE_COUNT + " > 0", null,
//                                  null, null,
//                                  StorageHelper.MESSAGE_CACHE_UPDATE_COUNT_DATE + " desc" );
        Cursor result = db.rawQuery( "select ul.activityName, ul.updateCount, ul.updateCountRefreshDate, mc.title from updatedLog ul " +
                                     "left outer join messageCache mc on mc.activityName = ul.activityName " +
                                     "where ul.updateCount > 0", null );
        rows = new ArrayList<DisplayNewRow>();

        TitleParser titleParser = new TitleParser();
        while( result.moveToNext() ) {
            String activityId = result.getString( 0 );
            int updateCount = result.getInt( 1 );
            long updateCountDate = result.getLong( 2 );
            String title = result.getString( 3 );

            String parsedTitle;
            if( title == null ) {
                parsedTitle = "empty";
            }
            else {
                parsedTitle = titleParser.parseTitle( title );
            }

            rows.add( new DisplayNewRow( activityId, parsedTitle, updateCount, updateCountDate ) );
        }
        result.close();
    }

    public DisplayNewRow getRowAndRemove( int position ) {
        DisplayNewRow row = rows.remove( position );

        SQLiteDatabase db = parentActivity.getDatabase();
        db.delete( StorageHelper.UPDATE_COUNT_TABLE,
                   StorageHelper.UPDATE_COUNT_ACTIVITY_NAME + " = ?", new String[] { row.activityId } );
        notifyDataSetChanged();
        
        return row;
    }
}
