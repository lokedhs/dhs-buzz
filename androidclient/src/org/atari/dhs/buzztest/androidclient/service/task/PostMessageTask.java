package org.atari.dhs.buzztest.androidclient.service.task;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.R;
import org.atari.dhs.buzztest.androidclient.buzz.BuzzManager;
import org.atari.dhs.buzztest.androidclient.buzz.jsonmodel.*;
import org.atari.dhs.buzztest.androidclient.service.*;
import org.atari.dhs.buzztest.androidclient.settings.PreferencesManager;
import org.atari.dhs.buzztest.androidclient.tools.PendingIntentFactory;

public class PostMessageTask implements ServiceTask
{
    public static final String COMMAND_NAME = "post";
    public static final String TEXT_KEY = "content";
    public static final String ATTACHED_URL_TITLE_KEY = "attachedUrlTitle";
    public static final String ATTACHED_URL_KEY = "attachedUrl";
    public static final String ATTACHED_IMAGES_KEY = "images";
    public static final String ATTACHED_IMAGE_URLS_KEY = "attachedImageUrls";
    public static final String GEOCODE_KEY = "geocode";
    public static final String PLACE_ID_KEY = "address";
    public static final String POST_PRIVATE_GROUPS_KEY = "groups";

    public static final String AUTH_TOKEN_TYPE = "lh2";

    @Override
    public void processTask( TaskContext taskContext, BundleX args ) throws ServiceTaskFailedException {
        String text = args.getString( TEXT_KEY );
        if( text == null ) {
            throw new IllegalArgumentException( "missing content" );
        }

        String attachedUrlTitle = args.getString( ATTACHED_URL_TITLE_KEY );
        String attachedUrl = args.getString( ATTACHED_URL_KEY );

        String[] images = args.getStringArray( ATTACHED_IMAGES_KEY );
        List<String> attachedImages = null;
        if( images != null && images.length >= 1 ) {
            attachedImages = uploadImages( taskContext, images, args );
        }

        String[] attachedImageUrls = args.getStringArray( ATTACHED_IMAGE_URLS_KEY );

        BuzzManager buzzManager = taskContext.getBuzzManager();
        BuzzActivity activity = new BuzzActivity();
        activity.object = new BuzzObject();
        activity.object.content = text;
        activity.object.attachments = makeAttachments( attachedUrlTitle, attachedUrl, attachedImages, attachedImageUrls );

//        activity.verbs = new ArrayList<String>();
//        activity.verbs.add( "share" );
//        activity.object.id = "tag:google.com,2010:buzz:z12dzn3z3vioyr3ch23ljxyayuasy3zd4";
//        activity.annotation = "This is my reshare annotation";

        String geocode = args.getString( GEOCODE_KEY );
        if( geocode != null ) {
            String placeId = args.getString( PLACE_ID_KEY );

            activity.geocode = geocode;
            activity.placeId = placeId;
        }

        String[] groups = args.getStringArray( POST_PRIVATE_GROUPS_KEY );
        if( groups != null ) {
            if( groups.length == 0 ) {
                throw new IllegalStateException( "If groups list is passed, it must contain at least one element" );
            }
            activity.visibility = new VisibilitySettings();
            activity.visibility.entries = new ArrayList<VisibilityEntry>();
            for( String group : groups ) {
                VisibilityEntry entry = new VisibilityEntry();
                entry.id = group;
                activity.visibility.entries.add( entry );
            }
        }

        activity.source = new BuzzActivitySource();
        activity.source.title = "DHS Buzz";

        try {
            Log.i( "posting activity:" + activity );
            buzzManager.postActivity( activity );
        }
        catch( IOException e ) {
            String msg = taskContext.getService().getResources().getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }
    }

    @Override
    public CharSequence getNotificationTickerText( Context context, BundleX args ) {
        Resources resources = context.getResources();
        String fmt = resources.getString( R.string.task_post_message_ticker );
        return MessageFormat.format( fmt, clampString( args.getString( TEXT_KEY ) ) );
    }

    @Override
    public boolean displayNotification() {
        return true;
    }

    private String clampString( String string ) {
        int maxLength = 30;
        if( string.length() <= maxLength ) {
            return string;
        }
        else {
            return string.substring( 0, maxLength - 3 ) + "\u2026";
        }
    }

    private List<Attachment> makeAttachments( String attachmentUrlTitle, String attachmentUrl, List<String> attachedImages, String[] attachedImageUrls ) {
        if( (attachedImages == null || attachedImages.isEmpty()) && attachmentUrl == null ) {
            return null;
        }

        List<Attachment> attachments = new ArrayList<Attachment>();
        if( attachmentUrl != null ) {
            Attachment attachment = new Attachment();
            attachment.type = Attachment.TYPE_ARTICLE;
            attachment.title = attachmentUrlTitle == null ? attachmentUrl : attachmentUrlTitle;

            attachment.links = new AttachmentLinks();
            attachment.links.alternate = new ArrayList<AttachmentLinkElement>();
            attachment.links.alternate.add( makeLinkElement( attachmentUrl, "type/html" ) );

            attachments.add( attachment );
        }

        if( attachedImages != null ) {
            for( String s : attachedImages ) {
                Attachment attachment = new Attachment();
                attachment.type = Attachment.TYPE_PHOTO;
                attachment.links = new AttachmentLinks();
                attachment.links.enclosure = new ArrayList<AttachmentLinkImageElement>();
                attachment.links.enclosure.add( makeImageLinkElement( s ) );
                attachments.add( attachment );
            }
        }

        if( attachedImageUrls != null ) {
            for( String s : attachedImageUrls ) {
                Attachment attachment = new Attachment();
                attachment.type = Attachment.TYPE_PHOTO;
                attachment.links = new AttachmentLinks();
                attachment.links.enclosure = new ArrayList<AttachmentLinkImageElement>();
                attachment.links.enclosure.add( makeImageLinkElement( s ) );
                attachments.add( attachment );
            }
        }

        return attachments;
    }

    private AttachmentLinkElement makeLinkElement( String href, String mimeType ) {
        AttachmentLinkElement elem = new AttachmentLinkElement();
        elem.href = href;
        elem.type = mimeType;
        return elem;
    }

    private AttachmentLinkImageElement makeImageLinkElement( String url ) {
        AttachmentLinkImageElement elem = new AttachmentLinkImageElement();
        elem.href = url;
        elem.type = "image/jpeg"; // hardcoded, since we know that picasa always publishes as JPEG
        return elem;
    }

    private List<String> uploadImages( TaskContext taskContext, String[] images, BundleX args ) throws ServiceTaskFailedException {
        AccountManager accountManager = AccountManager.get( taskContext.getService() );
        Account[] accounts = accountManager.getAccountsByType( "com.google" );

        PreferencesManager preferencesManager = new PreferencesManager( taskContext.getService() );
        String accountName = preferencesManager.getAccountName();

        if( accountName == null ) {
            throw new ServiceTaskFailedException( "no accounts configured" );
        }

        if( accounts == null ) {
            throw new ServiceTaskFailedException( "no accounts available" );
        }

        Account found = null;
        for( Account account : accounts ) {
            if( accountName.equals( account.name ) ) {
                found = account;
                break;
            }
        }

        if( found == null ) {
            Resources resources = taskContext.getService().getResources();
            String fmt = resources.getString( R.string.task_post_message_account_not_exist );
            String msg = MessageFormat.format( fmt, accountName );
            throw new RestartableServiceTaskFailedException( msg );
        }

        Bundle result;
        try {
            result = accountManager.getAuthToken( found, AUTH_TOKEN_TYPE, true, null, null ).getResult();
        }
        catch( OperationCanceledException e ) {
            throw new ServiceTaskFailedException( e );
        }
        catch( IOException e ) {
            Resources resources = taskContext.getService().getResources();
            String msg = resources.getString( R.string.error_communication );
            throw new RestartableServiceTaskFailedException( msg, e );
        }
        catch( AuthenticatorException e ) {
            Resources resources = taskContext.getService().getResources();
            String msg = resources.getString( R.string.error_google_acct_auth );
            throw new RestartableServiceTaskFailedException( msg, e );
        }

        if( result.containsKey( AccountManager.KEY_INTENT ) ) {
            Intent intent = PostMessageServiceImpl.createIntent( taskContext.getService(), COMMAND_NAME, args, false );
            throw new ReportableServiceTaskFailedException( "Accept the authentication request and select this to retry",
                                                            new PendingIntentFactory( intent, false ) );
        }
        else if( result.containsKey( AccountManager.KEY_AUTHTOKEN ) ) {
            try {
                List<String> attachedImageUrls = new ArrayList<String>();
                for( String imageName : images ) {
                    String url = postImage( taskContext, result.getString( AccountManager.KEY_AUTHTOKEN ), imageName );
                    attachedImageUrls.add( url );
                }
                return attachedImageUrls;
            }
            finally {
                accountManager.invalidateAuthToken( found.type, result.getString( AccountManager.KEY_AUTHTOKEN ) );
            }
        }
        else {
            throw new ServiceTaskFailedException( "authentication result does not contain the correct values" );
        }
    }

    private String postImage( TaskContext taskContext, String authToken, String imageFile ) throws ServiceTaskFailedException {
//        HttpTransport transport = GoogleTransport.create();
//        HttpRequest request = transport.buildPostRequest();

        AndroidHttpClient http = AndroidHttpClient.newInstance( "dhsBuzz", taskContext.getService() );
        try {
            HttpPost request = new HttpPost( "http://picasaweb.google.com/data/feed/api/user/default/albumid/default" );
            //        request.setHeader( "Authorization", "AuthSub token=\"" + authToken + "\"" );
            request.setHeader( "Authorization", "GoogleLogin auth=" + authToken );

            HttpEntity entity = new FileEntity( new File( imageFile ), "image/jpeg" );
            request.setEntity( entity );
            try {
                Log.i( "starting upload of file: " + imageFile );
                HttpResponse result = http.execute( request );
                Log.i( "upload done. resultCode=" + result.getStatusLine() );
                int statusCode = result.getStatusLine().getStatusCode();
                if( statusCode >= 200 && statusCode <= 204 ) {
                    HttpEntity resultEntity = result.getEntity();
                    Log.i( "result datatype=" + resultEntity.getContentType() );

                    DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
                    DocumentBuilder docBuilder = fac.newDocumentBuilder();
                    Document doc = docBuilder.parse( resultEntity.getContent() );

                    XPathFactory xpf = XPathFactory.newInstance();
                    XPath xPath = xpf.newXPath();

                    String url = (String)xPath.evaluate( "/entry/content/@src", doc, XPathConstants.STRING );
                    if( url.equals( "" ) ) {
                        throw new RestartableServiceTaskFailedException( "error uploading image to picasa" );
                    }
                    return url;
                }
                else {
                    Log.d( "error from picasa: " + result.getStatusLine() );
                    InputStream in = result.getEntity().getContent();
                    BufferedReader bufIn = new BufferedReader( new InputStreamReader( in, "UTF-8" ) );
                    String s;
                    while( (s = bufIn.readLine()) != null ) {
                        Log.i( "line:" + s );
                    }
                    throw new RestartableServiceTaskFailedException( "Error uploading image to picasa: " + result.getStatusLine() );
                }
            }
            catch( IOException e ) {
                Resources resources = taskContext.getService().getResources();
                String msg = resources.getString( R.string.error_communication );
                throw new RestartableServiceTaskFailedException( msg, e );
            }
            catch( ParserConfigurationException e ) {
                throw new ServiceTaskFailedException( "failed to upload image", e );
            }
            catch( SAXException e ) {
                throw new ServiceTaskFailedException( "failed to upload image", e );
            }
            catch( XPathExpressionException e ) {
                throw new ServiceTaskFailedException( "failed to upload image", e );
            }
        }
        finally {
            http.close();
        }
    }
}
