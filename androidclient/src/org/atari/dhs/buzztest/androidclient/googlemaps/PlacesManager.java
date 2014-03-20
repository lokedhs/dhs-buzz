package org.atari.dhs.buzztest.androidclient.googlemaps;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.location.Location;
import android.net.http.AndroidHttpClient;

import com.google.api.client.json.Json;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.GoogleMapsDetailsResult;
import org.atari.dhs.buzztest.androidclient.googlemaps.jsonmodel.GoogleMapsPlaceResult;
import org.codehaus.jackson.JsonParser;

public class PlacesManager
{
    private static final String CLIENT_ID = "gmws-dhsdevelopments.com";
    private static final String PRIVATE_KEY = "arqc_6zXe15f3v4FN4l6L-skp44=";

    public PlacesManager() {
    }

//    private String signUrl( String path ) throws NoSuchAlgorithmException {
//        Mac mac = Mac.getInstance( "HmacSHA1" );
//        SecretKeySpec keySpec = new SecretKeySpec( decodePrivateKey(), "HmacSHA1" );
//    }
//
//    private byte[] decodePrivateKey() {
//        UrlSigner
//    }

    public GoogleMapsPlaceResult placeSearch( Context context, Location location ) throws PlaceSearchFailedException {
        StringBuilder buf = new StringBuilder();
        buf.append( "https://maps.googleapis.com/maps/api/place/search/json?" );
        buf.append( "location=" );
        buf.append( location.getLatitude() );
        buf.append( "," );
        buf.append( location.getLongitude() );
        buf.append( "&radius=" );
        buf.append( Math.min( location.hasAccuracy() ? location.getAccuracy() : 20, 250 ) );
        buf.append( "&sensor=true" );
        buf.append( "&client=" );
        buf.append( CLIENT_ID );
        return loadFromUrl( context, buf.toString(), GoogleMapsPlaceResult.class );
    }

    public GoogleMapsDetailsResult locationSearch( Context context, String reference ) throws PlaceSearchFailedException {
        StringBuilder buf = new StringBuilder();
        buf.append( "https://maps.googleapis.com/maps/api/place/details/json?" );
        buf.append( "reference=" );
        buf.append( reference );
        buf.append( "&sensor=true" );
        buf.append( "&client=" );
        buf.append( CLIENT_ID );
        return loadFromUrl( context, buf.toString(), GoogleMapsDetailsResult.class );
    }

    @SuppressWarnings( { "ConstantConditions" })
    private <T> T loadFromUrl( Context context, String urlString, Class<T> cl ) throws PlaceSearchFailedException {
        try {
            URL url = new URL( urlString );

            UrlSigner signer = new UrlSigner( PRIVATE_KEY );
            String request = signer.signRequest( url.getPath(), url.getQuery() );

            AndroidHttpClient http = AndroidHttpClient.newInstance( "dhsBuzz", context );
            try {
                HttpGet httpGet = new HttpGet( url.getProtocol() + "://" + url.getHost() + request );
                HttpResponse result = http.execute( httpGet );
                if( result.getStatusLine().getStatusCode() != 200 ) {
                    Log.e( "error getting places: " + result.getStatusLine() );
                    BufferedReader in = new BufferedReader( new InputStreamReader( result.getEntity().getContent(), "UTF-8" ) );
                    String s;
                    while( (s = in.readLine()) != null ) {
                        Log.i( "line:" + s );
                    }
                    throw new PlaceSearchFailedException( "Error: " + result.getStatusLine() );
                }
                else {
                    JsonParser parser = Json.JSON_FACTORY.createJsonParser( result.getEntity().getContent() );
                    parser.nextToken();
                    T placeList = Json.parseAndClose( parser, cl, null );
                    return placeList;
                }
            }
            finally {
                http.close();
            }
        }
        catch( IOException e ) {
            throw new PlaceSearchFailedException( e );
        }
        catch( NoSuchAlgorithmException e ) {
            throw new PlaceSearchFailedException( e );
        }
        catch( InvalidKeyException e ) {
            throw new PlaceSearchFailedException( e );
        }
        catch( URISyntaxException e ) {
            throw new PlaceSearchFailedException( e );
        }
    }

    private static InputStream tstPRINT( InputStream content ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024 * 16];
        int n;
        while( (n = content.read( buf )) != -1 ) {
            out.write( buf, 0, n );
        }
        content.close();
        Log.i( "result:" + new String( out.toByteArray(), "UTF-8" ) );
        return new ByteArrayInputStream( out.toByteArray() );
    }
}
