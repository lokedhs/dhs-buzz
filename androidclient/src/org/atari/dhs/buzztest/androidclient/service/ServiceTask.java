package org.atari.dhs.buzztest.androidclient.service;

import android.content.Context;

public interface ServiceTask
{
    void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException;

    /**
     * Get the text to be displayed in the notification bar when the task is executing.
     * This message is also used when displaying a failed task in the error list activity.
     *
     * @param context
     *@param args the arguments for the task  @return the message text
     */
    CharSequence getNotificationTickerText( Context context, BundleX args );

    /**
     * Report if a notification icon should be displayed in the notification area.
     *
     * @return true if a notification should be displayed, and false if not
     */
    boolean displayNotification();
}
