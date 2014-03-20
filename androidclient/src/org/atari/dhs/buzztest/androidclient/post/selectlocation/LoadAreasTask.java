package org.atari.dhs.buzztest.androidclient.post.selectlocation;

import android.content.Context;
import android.location.Location;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.googlemaps.PlaceSearchFailedException;
import org.atari.dhs.buzztest.androidclient.googlemaps.PlacesManager;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.GoogleMapsPlaceResult;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;

class LoadAreasTask extends AsyncTaskWrapper<LoadAreasTask.Args, Void, LoadAreasTask.Result>
{
    @Override
    protected Result doInBackground( Args... argsList ) {
        Args args = argsList[0];

        Location location = args.location;
        try {
            Log.i( "requesting locations" );

            PlacesManager placesManager = new PlacesManager();
            GoogleMapsPlaceResult result = placesManager.placeSearch( args.context, args.location );

            return Result.makeSuccess( result );
        }
        catch( PlaceSearchFailedException e ) {
            Log.w( "exception when getting locations", e );
            return Result.makeError( e.getLocalizedMessage() );
        }
    }

    @Override
    protected void onPostExecute( Result result ) {
        super.onPostExecute( result );
        ((SelectLocationActivity)getUnderlyingActivity()).processLoadAreasResult( result );
    }

    static class Args
    {
        Location location;
        Context context;

        Args( Context context, Location location ) {
            this.context = context;
            this.location = location;
        }
    }

    static class Result
    {
        String errorMessage;
        GoogleMapsPlaceResult areasResult;

        static Result makeError( String errorMessage ) {
            Result result = new Result();
            result.errorMessage = errorMessage;
            return result;
        }

        static Result makeSuccess( GoogleMapsPlaceResult locations ) {
            Result result = new Result();
            result.areasResult = locations;
            return result;
        }
    }
}
