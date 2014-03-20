package org.atari.dhs.buzztest.androidclient;

import android.database.sqlite.SQLiteDatabase;

public class SQLiteDatabaseWrapper
{
    private DhsBuzzApp app;
    private SQLiteDatabase db;
    private Throwable closedLocation;

    SQLiteDatabaseWrapper( DhsBuzzApp app, SQLiteDatabase database ) {
        this.app = app;
        this.db = database;
    }

    public SQLiteDatabase getDatabase() {
        if( db == null ) {
            if( closedLocation == null ) {
                throw new IllegalStateException( "database has already been closed, no closed location" );
            }
            else {
                throw new IllegalStateException( "database has already been closed, closed location is cause", closedLocation );
            }
        }
        return db;
    }

    public void close() {
        app.releaseDatabaseWrapper( this );
        db = null;
        closedLocation = new Exception();
    }
}
