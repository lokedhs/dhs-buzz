package org.atari.dhs.buzztest.androidclient.googlemaps;

public class PlaceSearchFailedException extends Exception
{
    public PlaceSearchFailedException() {
    }

    public PlaceSearchFailedException( String s ) {
        super( s );
    }

    public PlaceSearchFailedException( String s, Throwable throwable ) {
        super( s, throwable );
    }

    public PlaceSearchFailedException( Throwable throwable ) {
        super( throwable );
    }
}
