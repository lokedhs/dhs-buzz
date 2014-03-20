package org.atari.dhs.buzztest.androidclient.failedtasks;

import android.view.View;

import org.atari.dhs.buzztest.androidclient.service.BundleX;

class FailedTaskRow
{
    public long id;
    public String title;
    public String message;
    public String command;
    public BundleX bundle;
    public View view;

    public FailedTaskRow( long id, String title, String message, String command, BundleX bundle ) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.command = command;
        this.bundle = bundle;
    }
}
