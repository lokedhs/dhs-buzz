package org.atari.dhs.buzztest.server.c2dm;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.atari.dhs.buzztest.common.C2DMSettings;
import org.atari.dhs.buzztest.server.BuzzConfig;
import org.atari.dhs.buzztest.server.HttpClientHelper;
import org.atari.dhs.buzztest.server.store.EMF;
import org.atari.dhs.buzztest.server.store.SubscribedFeed;
import org.atari.dhs.buzztest.server.store.UnsubscriptionRequest;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class C2DMServlet extends HttpServlet
{
    private static final Logger logger = Logger.getLogger( C2DMServlet.class.getName() );

    private DocumentBuilderFactory documentBuilderFactory;
    private XPathFactory xPathFactory;

    @Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );

        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        xPathFactory = XPathFactory.newInstance();
    }

    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        String command = request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_COMMAND );
        String activityId = request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_ACTIVITY );
        String c2dmKey = request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_KEY );
        String userName = request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_USER_NAME );
        String userId = request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_USER_ID );

        if( !checkActivityName( activityId ) || !checkC2DMKey( c2dmKey ) ) {
            throw new ServletException( "incorrect activity name or c2dmKey" );
        }

        boolean subscribe;
        if( C2DMSettings.C2DM_COMMAND_VALUE_SUBSCRIBE.equals( command ) ) {
            subscribe = true;
        }
        else if( C2DMSettings.C2DM_COMMAND_VALUE_UNSUBSCRIBE.equals( command ) ) {
            subscribe = false;
        }
        else {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
            return;
        }

        logger.info( "got activity: " + activityId + ", key=" + c2dmKey + ", subscribe=" + subscribe );

        String activityUrl = buildActivityUrl( userId, activityId );

        if( subscribe ) {
            subscribeToActivity( activityId, activityUrl, c2dmKey, userName, userId );
        }
        else {
            unsubscribeFromActivity( activityId, c2dmKey );
        }

        response.setStatus( HttpServletResponse.SC_OK );
    }

    private static String buildActivityUrl( String userId, String activityId ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "https://www.googleapis.com/buzz/v1/" );
        buf.append( "activities/" );
        buf.append( userId );
        buf.append( "/@self/" );
        buf.append( activityId );
        return buf.toString();
    }

    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        response.setContentType( "text/plain" );
        response.getWriter().println( "failed" );
    }

    private boolean checkActivityName( String activity ) {
        return !(activity == null || activity.length() == 0);
    }

    private boolean checkC2DMKey( String c2dmKey ) {
        return !(c2dmKey == null || c2dmKey.length() == 0);
    }

    private void subscribeToActivity( String activityId, String activityUrl, String c2dmKey, String userName, String userId ) throws ServletException {
        URLFetchService urlFetch = URLFetchServiceFactory.getURLFetchService();
        try {
            String commentsUrl = loadCommentsUrl( urlFetch, stripUrlSuffix( activityUrl ) );
            String fixedCommentsUrl = stripUrlSuffix( commentsUrl );

            HTTPResponse result = urlFetch.fetch( new URL( appendKey( fixedCommentsUrl ) ) );
            if( result.getResponseCode() != 200 ) {
                throw new ServletException( "error result from feed: " + result.getResponseCode() );
            }

            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse( new ByteArrayInputStream( result.getContent() ) );

            XPath xPath = xPathFactory.newXPath();
            String href = (String)xPath.evaluate( "/feed/link[@rel='hub']/@href", doc, XPathConstants.STRING );
            if( href == null ) {
                throw new ServletException( "can't find hub url in document" );
            }

            logger.info( "Got href: " + href );

            String verificationId = createVerificationToken( activityId, fixedCommentsUrl, c2dmKey, href, userName, userId );
            if( verificationId != null ) {
                sendPubSubRequest( href, fixedCommentsUrl, verificationId, true );
            }
        }
        catch( MalformedURLException e ) {
            throw new ServletException( "exception when processing feed", e );
        }
        catch( IOException e ) {
            throw new ServletException( "exception when processing feed", e );
        }
        catch( ParserConfigurationException e ) {
            throw new ServletException( "exception when processing feed", e );
        }
        catch( SAXException e ) {
            throw new ServletException( "exception when processing feed", e );
        }
        catch( XPathExpressionException e ) {
            throw new ServletException( "exception when processing feed", e );
        }
    }

    private String appendKey( String url ) {
        return url + "?key=" + BuzzConfig.SERVER_BUZZ_API_KEY;
    }

    private String stripUrlSuffix( String url ) {
        int i = url.indexOf( "?" );
        if( i == -1 ) {
            return url;
        }
        else {
            return url.substring( 0, i );
        }
    }

    private String loadCommentsUrl( URLFetchService urlFetch, String fixedUrl ) throws IOException, ServletException, ParserConfigurationException, SAXException, XPathExpressionException {
        HTTPResponse result = urlFetch.fetch( new URL( appendKey( fixedUrl ) ) );
        if( result.getResponseCode() != 200 ) {
            throw new ServletException( "error result when getting feed: " + result.getResponseCode() );
        }

        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse( new ByteArrayInputStream( result.getContent() ) );

        XPath xPath = xPathFactory.newXPath();
        String href = (String)xPath.evaluate( "/entry/link[@rel='replies']/@href", doc, XPathConstants.STRING );
        if( href == null || href.equals( "" ) ) {
            throw new ServletException( "missing replies link in feed" );
        }

        return href;
    }

    private void sendPubSubRequest( String hubUrl, String commentsUrl, String verificationId, boolean subscribe ) throws ServletException {
        try {
            HttpClient httpClient = HttpClientHelper.createHttpClient();

            HttpPost httpPost = new HttpPost( hubUrl );
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add( new BasicNameValuePair( "hub.callback", "https://lokemaptest.appspot.com/PushMessageAndroid" ) );
            params.add( new BasicNameValuePair( "hub.mode", subscribe ? "subscribe" : "unsubscribe" ) );
            params.add( new BasicNameValuePair( "hub.topic", commentsUrl ) );
            params.add( new BasicNameValuePair( "hub.verify", "async" ) );
            params.add( new BasicNameValuePair( "hub.verify_token", verificationId ) );
            httpPost.setEntity( new UrlEncodedFormEntity( params ) );

            HttpResponse httpResponse = httpClient.execute( httpPost );
            HttpEntity httpEntity = httpResponse.getEntity();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if( statusCode != HttpServletResponse.SC_OK && statusCode != HttpServletResponse.SC_ACCEPTED ) {
                throw new ServletException( "subscription request failed: " + statusCode );
            }
        }
        catch( UnsupportedEncodingException e ) {
            throw new ServletException( "exception when posting subscription request", e );
        }
        catch( ClientProtocolException e ) {
            throw new ServletException( "exception when posting subscription request", e );
        }
        catch( IOException e ) {
            throw new ServletException( "exception when posting subscription request", e );
        }
    }

    private String createVerificationToken( String activityId, String commentsUrl, String c2dmKey, String hubUrl, String userName, String userId ) {
        Date now = new Date();

        StringBuilder buf = new StringBuilder();
        for( int i = 0 ; i < 30 ; i++ ) {
            buf.append( (char)((Math.random() * ('z' - 'a')) + 'a') );
        }

        EntityTransaction tx = null;
        EntityManager entityManager = EMF.get().createEntityManager();
        List result = findSubscribedFeed( activityId, c2dmKey, entityManager );
        if( !result.isEmpty() ) {
            logger.log( Level.INFO, "attempt to subscribe to already subscribed feed. activityId=" + activityId + ", c2dmKey=" + c2dmKey );
            return null;
        }

        try {
            tx = entityManager.getTransaction();
            tx.begin();

            SubscribedFeed subscribedFeed = new SubscribedFeed();

            subscribedFeed.setActivityId( activityId );
            subscribedFeed.setCommentsUrl( commentsUrl );
            subscribedFeed.setC2dmKey( c2dmKey );
            subscribedFeed.setCreatedDate( now );
            subscribedFeed.setVerificationId( buf.toString() );
            subscribedFeed.setVerified( false );
            subscribedFeed.setHubUrl( hubUrl );
            subscribedFeed.setUserName( userName );
            subscribedFeed.setUserId( userId );

            entityManager.persist( subscribedFeed );

            String token = subscribedFeed.getVerificationId();

            tx.commit();
            tx = null;

            return token;
        }
        finally {
            if( tx != null ) {
                tx.rollback();
            }

            entityManager.close();
        }
    }

    private void unsubscribeFromActivity( String activityId, String c2dmKey ) throws ServletException {
        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            List result = findSubscribedFeed( activityId, c2dmKey, entityManager );
            Query query;
            if( result.isEmpty() ) {
                logger.log( Level.INFO, "attempt to unsubscribe with no active subscription" );
                return;
            }

            if( result.size() > 1 ) {
                logger.log( Level.WARNING, "multiple subscriptions for the same key: activity=" + activityId + ", c2DMKey=" + c2dmKey );
            }

            SubscribedFeed subscribedFeed = (SubscribedFeed)result.get( 0 );
            String commentsUrl = subscribedFeed.getCommentsUrl();
            String verificationId = subscribedFeed.getVerificationId();
            String hubUrl = subscribedFeed.getHubUrl();

            List<Long> toBeRemoved = new ArrayList<Long>();
            for( Object o : result ) {
                SubscribedFeed f = (SubscribedFeed)o;
                toBeRemoved.add( f.getId() );
            }

            for( long id : toBeRemoved ) {
                query = entityManager.createNamedQuery( "removeSubscribedFeedById" );
                query.setParameter( "id", id );
                query.executeUpdate();
            }

            //noinspection UnusedCatchParameter
            try {
                UnsubscriptionRequest prev = entityManager.find( UnsubscriptionRequest.class, commentsUrl );
                if( prev != null ) {
                    entityManager.remove( prev );
                }
            }
            catch( NoResultException e ) {
                // No-op, since this is the expected result
            }

            query = entityManager.createNamedQuery( "findSubscribedFeedCountByActivityUrl" );
            query.setParameter( "activityUrl", activityId );
            int subscribers = (Integer)query.getSingleResult();
            logger.info( "found " + subscribers + " subscribers" );

            if( subscribers == 0 ) {
                UnsubscriptionRequest unsubscriptionRequest = new UnsubscriptionRequest( commentsUrl, verificationId );
                entityManager.persist( unsubscriptionRequest );

                sendPubSubRequest( hubUrl, commentsUrl, verificationId, false );
            }
        }
        finally {
            entityManager.close();
        }
    }

    private List findSubscribedFeed( String activityId, String c2dmKey, EntityManager entityManager ) {
        Query query = entityManager.createNamedQuery( "findSubscribedFeedByActivityUrlAndC2DMKey" );
        query.setParameter( "activityUrl", activityId );
        query.setParameter( "c2DMKey", c2dmKey );
        return query.getResultList();
    }
}
