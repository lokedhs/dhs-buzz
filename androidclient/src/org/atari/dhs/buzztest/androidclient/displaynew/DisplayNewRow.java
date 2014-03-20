package org.atari.dhs.buzztest.androidclient.displaynew;

public class DisplayNewRow
{
    public String activityId;
    public String title;
    public int updateCount;
    public long updateCountDate;

    public DisplayNewRow( String activityId, String title, int updateCount, long updateCountDate ) {
        this.activityId = activityId;
        this.title = title;
        this.updateCount = updateCount;
        this.updateCountDate = updateCountDate;
    }
}
