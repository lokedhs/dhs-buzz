package org.atari.dhs.buzztest.androidclient.tools;

import android.content.Context;
import android.widget.ArrayAdapter;

public class OptionArrayAdapter<T> extends ArrayAdapter<OptionArrayAdapter.ElementWrapper<T>>
{
    public OptionArrayAdapter( Context context ) {
        super( context, android.R.layout.simple_spinner_item );
        setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item );
    }

    public void addItemWithName( T item, int descriptionResource ) {
        Context context = getContext();
        addItemWithName(item, context.getResources().getString( descriptionResource ) );
    }

    public void addItemWithName( T item, String description ) {
        add( new ElementWrapper<T>( item, description ) );
    }

    public T getItemFromPosition( int position ) {
        return getItem( position ).element;
    }

    public int getIndexForItem( T item ) {
        int n = getCount();
        for( int i = 0 ; i < n ; i++ ) {
            if( getItem( i ).element.equals( item ) ) {
                return i;
            }
        }

        throw new IllegalStateException( "item not found in adapter: " + item );
    }

    public static class ElementWrapper<T>
    {
        private T element;
        private String description;

        private ElementWrapper( T element, String description ) {
            this.element = element;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
