package org.atari.dhs.buzztest.androidclient.translation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.net.http.AndroidHttpClient;

import com.google.api.client.json.Json;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.atari.dhs.buzztest.androidclient.DhsBuzzApp;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;
import org.atari.dhs.buzztest.androidclient.translation.jsonmodel.LanguageDetectResult;
import org.atari.dhs.buzztest.androidclient.translation.jsonmodel.TranslationResult;
import org.codehaus.jackson.JsonParser;

class TranslateTask extends AsyncTaskWrapper<TranslateTask.Args, Void, TranslateTask.Result>
{
    @Override
    protected Result doInBackground( Args... argsList ) {
        Args args = argsList[0];

        try {
            AndroidHttpClient client = AndroidHttpClient.newInstance( "dhsBuzz" );
            try {
                String sourceLanguage = args.sourceLanguage;
                if( sourceLanguage == null ) {
                    sourceLanguage = detectLanguage( client, args.text );
                    if( sourceLanguage == null ) {
                        // TODO: hardcoded text
                        return Result.makeError( "Language could not be detected. Try choosing a language from the popup." );
                    }
                }

                HttpPost request = new HttpPost( "https://ajax.googleapis.com/ajax/services/language/translate" );
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add( new BasicNameValuePair( "v", "1.0" ) );
                params.add( new BasicNameValuePair( "q", args.text ) );
                params.add( new BasicNameValuePair( "langpair", sourceLanguage + "|" + args.destLanguage ) );
//                params.add( new BasicNameValuePair( "key", DhsBuzzApp.API_KEY ) );
                HttpEntity entity = new UrlEncodedFormEntity( params, "UTF-8" );
                request.setEntity( entity );
                HttpResponse result = client.execute( request );

                if( result.getStatusLine().getStatusCode() == 200 ) {
                    JsonParser parser = Json.JSON_FACTORY.createJsonParser( result.getEntity().getContent() );
                    parser.nextToken();
                    TranslationResult translationResult = Json.parseAndClose( parser, TranslationResult.class, null );
                    Log.i( "translation result: " + translationResult );
                    if( translationResult.responseData != null ) {
                        return Result.makeSuccess( translationResult.responseData.translatedText, args.sourceLanguage == null ? sourceLanguage : null );
                    }
                    else {
                        // TODO: hardcoded text
                        return Result.makeError( "responseData was null" );
                    }
                }
                else {
                    return Result.makeError( result.getStatusLine().getReasonPhrase() );
                }
            }
            finally {
                client.close();
            }
        }
        catch( IOException e ) {
            Log.w( "error downloading result", e );
            return Result.makeError( e.getLocalizedMessage() );
        }
    }

    private String detectLanguage( AndroidHttpClient client, String text ) throws IOException {
        StringBuilder buf = new StringBuilder();
//        buf.append( "https://ajax.googleapis.com/ajax/services/language/detect?key=" );
//        buf.append( DhsBuzzApp.API_KEY );
//        buf.append( "&v=1.0&q=" );
        buf.append( "https://ajax.googleapis.com/ajax/services/language/detect?v=1.0&q=" );
        int limit = 70;
        String croppedResult = text;
        if( croppedResult.length() > limit ) {
            croppedResult = text.substring( 0, limit );
        }
        buf.append( URLEncoder.encode( croppedResult, "UTF-8" ) );
        HttpGet request = new HttpGet( buf.toString() );
        HttpResponse result = client.execute( request );
        if( result.getStatusLine().getStatusCode() != 200 ) {
            Log.w( "failed to detect language: " + result.getStatusLine() );
            return null;
        }

        JsonParser parser = Json.JSON_FACTORY.createJsonParser( result.getEntity().getContent() );
        parser.nextToken();
        LanguageDetectResult translationResult = Json.parseAndClose( parser, LanguageDetectResult.class, null );
        return translationResult.responseData.language;
    }

    private InputStream tstPRINT( InputStream content ) throws IOException {
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

    @Override
    protected void onPostExecute( Result result ) {
        super.onPostExecute( result );

        ((TranslateActivity)getUnderlyingActivity()).processResult( result );
    }

    static class Args
    {
        String text;
        String sourceLanguage;
        String destLanguage;

        Args( String text, String sourceLanguage, String destLanguage ) {
            this.text = text;
            this.sourceLanguage = sourceLanguage;
            this.destLanguage = destLanguage;
        }
    }

    static class Result
    {
        String translatedMessage;
        String errorMessage;
        String detectedLanguage;

        static Result makeSuccess( String translatedMessage, String detectedLanguage ) {
            Result result = new Result();
            result.translatedMessage = translatedMessage;
            result.detectedLanguage = detectedLanguage;
            return result;
        }

        static Result makeError( String errorMessage ) {
            Result result = new Result();
            result.errorMessage = errorMessage;
            return result;
        }
    }
}
