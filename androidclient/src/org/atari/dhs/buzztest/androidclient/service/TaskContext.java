package org.atari.dhs.buzztest.androidclient.service;

import android.app.Service;
import android.database.sqlite.SQLiteDatabase;

import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;

public interface TaskContext
{
    BuzzManager getBuzzManager();

    SQLiteDatabase getDatabase();

    Service getService();
}
