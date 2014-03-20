package org.atari.dhs.buzztest.androidclient.userlist;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.usersdetail.User;

class UserListViewAdapter extends BaseAdapter
{
    private LayoutInflater inflater;
    private List<Row> users;

    public UserListViewAdapter( LayoutInflater inflater ) {
        this.inflater = inflater;
    }

    public void setUsers( List<User> users ) {
        if( users == null ) {
            this.users = new ArrayList<Row>();
        }
        else {
            this.users = new ArrayList<Row>( users.size() );
            for( User user : users ) {
                this.users.add( new Row( user ) );
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return users == null ? 1 : users.size();
    }

    @Override
    public Object getItem( int position ) {
        return users == null ? null : users.get( position );
    }

    @Override
    public long getItemId( int position ) {
        return position;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        if( users == null ) {
            if( position != 0 ) {
                throw new IllegalStateException( "only position zero is allowed while loading" );
            }
            return getLoadingPanel( convertView, parent );
        }
        else {
            return getContentPanel( position, convertView, parent );
        }
    }

    private View getLoadingPanel( View convertView, ViewGroup parent ) {
        View v;
        if( convertView != null && convertView.getId() == R.id.user_list_loading_panel_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.user_list_loading_panel, parent, false );
        }
        return v;
    }

    private View getContentPanel( int position, View convertView, ViewGroup parent ) {
        View v;
        if( convertView != null && convertView.getId() == R.id.user_list_content_panel_view ) {
            v = convertView;
        }
        else {
            v = inflater.inflate( R.layout.user_list_content_panel, parent, false );
        }

        Row row = users.get( position );

        TextView nameTextView = (TextView)v.findViewById( R.id.name_text_view );
        nameTextView.setText( row.user.displayName );
        return v;
    }

    public Row getRowByPosition( int position ) {
        return users == null ? null : users.get( position );
    }
}
