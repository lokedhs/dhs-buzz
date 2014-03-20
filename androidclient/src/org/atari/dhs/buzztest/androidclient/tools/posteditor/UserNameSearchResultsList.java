package org.atari.dhs.buzztest.androidclient.tools.posteditor;

import java.util.ArrayList;
import java.util.List;

class UserNameSearchResultsList
{
    private List<UserNameSearchResult> results = new ArrayList<UserNameSearchResult>();

    public void addResult( UserNameSearchResult result ) {
        results.add( result );
    }

    public List<UserNameSearchResult> getResults() {
        return results;
    }
}
