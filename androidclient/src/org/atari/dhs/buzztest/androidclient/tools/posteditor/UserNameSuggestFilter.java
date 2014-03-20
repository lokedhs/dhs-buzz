package org.atari.dhs.buzztest.androidclient.tools.posteditor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.widget.Filter;

class UserNameSuggestFilter extends Filter
{
    private Context context;
    private UserNameSuggestAdapter userNameSuggestAdapter;

    public UserNameSuggestFilter( Context context, UserNameSuggestAdapter userNameSuggestAdapter ) {
        this.context = context;
        this.userNameSuggestAdapter = userNameSuggestAdapter;
    }

    @Override
    protected FilterResults performFiltering( CharSequence text ) {
        if( text == null ) {
            return null;
        }

        ContentResolver contentResolver = context.getContentResolver();

        String nameLike = text + "%";
        String emailLike = text + "%@gmail.com";

        String whereClause = "(" + ContactsContract.Contacts.DISPLAY_NAME + " like ?"
                             + " and " + ContactsContract.CommonDataKinds.Email.DATA + " like '%gmail.com')"
                             + " or " + ContactsContract.CommonDataKinds.Email.DATA + " like ?";

        Cursor result = contentResolver.query( ContactsContract.Data.CONTENT_URI,
                                               new String[] { ContactsContract.Contacts.DISPLAY_NAME,
                                                              ContactsContract.CommonDataKinds.Email.DATA },
                                               whereClause, new String[] { nameLike, emailLike },
                                               ContactsContract.Contacts.DISPLAY_NAME );

        UserNameSearchResultsList results = new UserNameSearchResultsList();
        while( result.moveToNext() ) {
            String name = result.getString( 0 );
            String email = result.getString( 1 );
            results.addResult( new UserNameSearchResult( name, email ) );
        }

        FilterResults filterResults = new FilterResults();
        filterResults.values = results;
        filterResults.count = results.getResults().size();
        return filterResults;
    }

    @Override
    protected void publishResults( CharSequence charSequence, FilterResults filterResults ) {
        if( filterResults != null ) {
            userNameSuggestAdapter.setSuggestions( (UserNameSearchResultsList)filterResults.values );
        }
    }
}
