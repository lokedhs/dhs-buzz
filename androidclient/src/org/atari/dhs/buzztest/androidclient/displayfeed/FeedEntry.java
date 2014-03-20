package org.atari.dhs.buzztest.androidclient.displayfeed;

import java.util.Date;

import org.atari.dhs.buzztest.androidclient.buzz.CachedFeedActivity;
import org.atari.dhs.buzztest.androidclient.tools.DateHelper;

public class FeedEntry
{
    private CachedFeedActivity activity;
    private DateHelper dateHelper;
    private Date updatedDate;
    private String formattedDate;

    public FeedEntry( CachedFeedActivity activity, DateHelper dateHelper ) {
        this.activity = activity;
        this.dateHelper = dateHelper;
    }

    public CachedFeedActivity getActivity() {
        return activity;
    }

    public Date getUpdatedDate() {
        if( updatedDate == null ) {
            updatedDate = new Date( activity.getUpdated() );
        }
        return updatedDate;
    }

    public String getFormattedDate() {
        if( formattedDate == null ) {
            Date date = getUpdatedDate();
            formattedDate = dateHelper.formatDateTimeOutputFormat( date );
        }
        return formattedDate;
    }
}
