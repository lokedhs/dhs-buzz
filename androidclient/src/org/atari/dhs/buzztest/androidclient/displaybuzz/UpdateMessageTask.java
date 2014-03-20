package org.atari.dhs.buzztest.androidclient.displaybuzz;

import android.database.sqlite.SQLiteDatabase;

import org.atari.dhs.buzztest.androidclient.SQLiteDatabaseWrapper;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class UpdateMessageTask extends AsyncTaskWrapper<UpdateMessageTask.Args,Void,Void>
{
    private Args args;

    @Override
    protected Void doInBackground( Args... argsList ) {
        this.args = argsList[0];

        SQLiteDatabase db = args.db.getDatabase();
        db.beginTransaction();
//        try {
//            db.query( StorageHelper.MESSAGE_CACHE_TABLE,
//                      new String[] { StorageHelper.MESSAGE_CACHE_UPDATE_COUNT}, )
//        }
//        finally {
//            db.endTransaction();
//        }

        throw new UnsupportedOperationException( "background loading of messages not supported" );
    }

    @Override
    protected void onPostExecute( Void arg ) {
        super.onPostExecute( arg );
        args.db.close();
    }

    static class Args
    {
        SQLiteDatabaseWrapper db;
        BuzzActivity buzz;

        Args( SQLiteDatabaseWrapper db, BuzzActivity buzz ) {
            this.db = db;
            this.buzz = buzz;
        }
    }
}
