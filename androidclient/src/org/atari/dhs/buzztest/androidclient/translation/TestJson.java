package org.atari.dhs.buzztest.androidclient.translation;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

import com.google.api.client.json.Json;
import com.google.api.client.util.GenericData;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzActivity;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.BuzzObject;
import org.codehaus.jackson.JsonGenerator;

/**
 * Test class to experiment with the JSON API.
 */
public class TestJson
{
    public static void main( String[] args ) {
        try {
//            testParse();
            testGenerate();
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    private static void testGenerate() throws IOException {
        BuzzActivity activity = new BuzzActivity();
        activity.object = new BuzzObject();
        activity.object.content = "test content";
        activity.id = "the id";
        activity.annotation = "annotation";
        activity.verbs = new ArrayList<String>();
        activity.verbs.add( "verb" );

        StringWriter out = new StringWriter();
        JsonGenerator generator = Json.JSON_FACTORY.createJsonGenerator( out );
        Json.serialize( generator, activity );
        generator.flush();

        System.out.println( "result=" + out.toString() );
    }

//    private static void testParse() throws IOException {
//        HttpTransport transport = new HttpTransport();
//
////            StringBuilder buf = new StringBuilder();
////            buf.append( "https://ajax.googleapis.com/ajax/services/language/translate?v=1.0&q=" );
////            buf.append( URLEncoder.encode( "hej alla", "UTF-8" ) );
////            buf.append( "&langpair=" );
////            buf.append( URLEncoder.encode( "sv|en", "UTF-8" ) );
//
////            HttpRequest request = transport.buildPostRequest();
////            System.out.println( "translation service url=" + buf.toString() );
////            request.setUrl( "https://ajax.googleapis.com/ajax/services/language/translate" );
////            request.content = new UrlEncodedContent();
////            HttpResponse result = request.execute();
//
//        AndroidHttpClient client = AndroidHttpClient.newInstance( "dhsBuzz" );
//        HttpPost request = new HttpPost( "https://ajax.googleapis.com/ajax/services/language/translate" );
//        List<NameValuePair> params = new ArrayList<NameValuePair>();
//        params.add( new BasicNameValuePair( "v", "1.0" ) );
//        params.add( new BasicNameValuePair( "q", "hej till alla" ) );
//        params.add( new BasicNameValuePair( "langpair", "sv|en" ) );
//        HttpEntity entity = new UrlEncodedFormEntity( params );
//        request.setEntity( entity );
//        HttpResponse result = client.execute( request );
//
//        if( result.getStatusLine().getStatusCode() == 200 ) {
//            JsonParser parser = Json.JSON_FACTORY.createJsonParser( tstPRINT( result.getEntity().getContent() ) );
//
////                String s = "{\"responseData\": {\"translatedText\":\"hi all\"}, \"responseDetails\": \"abc\", \"responseStatus\": 200}";
////                String s = "{\"data\":{\"responseDetails\":\"abc\", \"responseStatus\": 200}}";
//
//            String s = "{\"responseData\": {\"translatedText\":\"hi all\"}, \"responseDetails\": \"abc\", \"responseStatus\": 200}";
////                  String s = "{\"responseData\": {\"responseDetails\":\"foo\"}, \"aa\":\"bb\", \"bar\":200}";
////                JsonParser parser = Json.JSON_FACTORY.createJsonParser( new ByteArrayInputStream( s.getBytes( "UTF-8" ) ) );
//            parser.nextToken();
//
//            TranslationResult translationResult = Json.parseAndClose( parser, TranslationResult.class, null );
//            System.out.println( "translation result: " + translationResult );
//        }
//        else {
//            System.out.println( "Error: " + result.getStatusLine() );
//        }
//    }

    private static void printGenericData( GenericData g ) {
        System.out.println( "printing objects" );
        for( Map.Entry<String, Object> e : g.entrySet() ) {
            System.out.printf( "key=%s, value=%s%n", e.getKey(), e.getValue() );
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
        System.out.println( "result:" + new String( out.toByteArray(), "UTF-8" ) );
        return new ByteArrayInputStream( out.toByteArray() );
    }
}
