package org.atari.dhs.buzztest.androidclient.post.selectlocation;

import android.content.Context;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.googlemaps.PlaceSearchFailedException;
import org.atari.dhs.buzztest.androidclient.googlemaps.PlacesManager;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.GoogleMapsDetailsResult;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.GoogleMapsPlace;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadLocationsTask extends AsyncTaskWrapper<LoadLocationsTask.Args,Void,LoadLocationsTask.Result>
{
    @Override
    protected Result doInBackground( Args... argsList ) {
        Args args = argsList[0];

        PlacesManager placesManager = new PlacesManager();
        try {
            GoogleMapsDetailsResult result = placesManager.locationSearch( args.context, args.location.reference );
            return new Result( result );
        }
        catch( PlaceSearchFailedException e ) {
            Log.w( "error loading result", e );
            return new Result( e.getMessage() );
        }
    }

    @Override
    protected void onPostExecute( Result result ) {
        ((SelectLocationActivity)getUnderlyingActivity()).processLoadLocationsResult( result );
    }

    static class Args
    {
        Context context;
        GoogleMapsPlace location;

        public Args( Context context, GoogleMapsPlace location ) {
            this.context = context;
            this.location = location;
        }
    }

    static class Result
    {
        String errorMessage;
        GoogleMapsDetailsResult locations;

        Result( GoogleMapsDetailsResult locations ) {
            this.locations = locations;
        }

        Result( String errorMessage ) {
            this.errorMessage = errorMessage;
        }
    }
}
