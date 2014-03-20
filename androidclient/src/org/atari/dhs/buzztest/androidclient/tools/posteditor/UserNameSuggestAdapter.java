package org.atari.dhs.buzztest.androidclient.tools.posteditor;

import java.util.List;

import android.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

class UserNameSuggestAdapter extends BaseAdapter implements Filterable
{
    private Context context;
    private LayoutInflater inflater;
    private List<UserNameSearchResult> suggestions;

    UserNameSuggestAdapter( Context context ) {
        this.context = context;
        inflater = (LayoutInflater)context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
    }

    @Override
    public int getCount() {
        int ret = suggestions == null ? 0 : suggestions.size();
        return ret;
    }

    @Override
    public Object getItem( int i ) {
        return suggestions.get( i );
    }

    @Override
    public long getItemId( int i ) {
        return i;
    }

    @Override
    public View getView( int position, View convertView, ViewGroup parent ) {
        TextView v;
        if( convertView != null && convertView instanceof TextView ) {
            v = (TextView)convertView;
        }
        else {
            v = (TextView)inflater.inflate( R.layout.simple_dropdown_item_1line, parent, false );
        }

        UserNameSearchResult result = suggestions.get( position );

        v.setText( result.getName() + " (" + result.getEmail() + ")" );

        return v;
    }

    @Override
    public Filter getFilter() {
        Filter ret = new UserNameSuggestFilter( context, this );
        return ret;
    }

    public void setSuggestions( UserNameSearchResultsList values ) {
        suggestions = values.getResults();
        notifyDataSetChanged();
    }
}
