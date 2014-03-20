package org.atari.dhs.buzztest.server.c2dm.feed;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.xml.sax.SAXException;

import org.atari.dhs.buzztest.common.C2DMSettings;
import org.atari.dhs.buzztest.server.HttpClientHelper;
import org.atari.dhs.buzztest.server.c2dm.ClientLoginKeyManager;
import org.atari.dhs.buzztest.server.store.EMF;
import org.atari.dhs.buzztest.server.store.feed.GenericFeedSubscription;
import org.atari.dhs.buzztest.server.store.feed.Subscriber;
import org.atari.dhs.buzztest.server.tests.TimeDiff;

public class FeedMessageAndroidServlet extends HttpServlet
{
    private static final Logger logger = Logger.getLogger( FeedMessageAndroidServlet.class.getName() );

    private static final String FORCE_TOKEN_NAME = "FORCE";

    private FeedUpdateParser feedUpdateParser;
    private ClientLoginKeyManager clientLoginKeyManager;

    @Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );

        feedUpdateParser = new FeedUpdateParser();
        clientLoginKeyManager = new ClientLoginKeyManager();
    }

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        logRequest( "FEED MESSAGE:GET", req );

        String mode = req.getParameter( "hub.mode" );
        if( "subscribe".equals( mode ) ) {
            replyToSubscriptionRequest( req, resp );
        }
        else if( "unsubscribe".equals( mode ) ) {
            replyToUnsubscriptionRequest( req, resp );
        }
        else {
            logger.warning( "unknown mode: " + mode );
        }
    }

    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
        logRequest( "FEED MESSAGE:POST", req );

        try {
            String feedId = req.getParameter( FeedSubscriberServlet.PATH_ID_PARAM_NAME );
            if( feedId == null || feedId.equals( "" ) ) {
                logger.log( Level.WARNING, "post without feedId" );
                resp.setStatus( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            List<GenericFeedSubscription> removedFeedList = new ArrayList<GenericFeedSubscription>();
            EntityManager entityManager = EMF.get().createEntityManager();
            try {
                EntityTransaction tx = entityManager.getTransaction();
                tx.begin();
                try {
                    GenericFeedSubscription feedSub = entityManager.find( GenericFeedSubscription.class, feedId );
                    if( feedSub == null ) {
                        logger.log( Level.WARNING, "no subscription for feed: " + feedId + ". Will unsubscribe." );
                        FeedSubscriberServlet.sendPubSubRequest( "http://pubsubhubbub.appspot.com/",
                                                                 req.getParameter( FeedSubscriberServlet.PATH_ID_PARAM_NAME ),
                                                                 FORCE_TOKEN_NAME,
                                                                 false );
                        return;
                    }

                    if( feedSub.isDeleted() ) {
                        logger.log( Level.WARNING, "feed update for deleted feed: " + feedId );
                        resp.setStatus( HttpServletResponse.SC_NOT_FOUND );
                        return;
                    }

                    FeedUpdateInfo info = feedUpdateParser.parse( req.getInputStream() );

                    HttpClient httpClient = HttpClientHelper.createHttpClient();

                    long subscriptionCutoff = System.currentTimeMillis() - TimeDiff.DAY_MILLIS;
                    long refreshCutoff = System.currentTimeMillis() - TimeDiff.HOUR_MILLIS;

                    Collection<Subscriber> subscribers = feedSub.getSubscribers();
                    for( Subscriber subscriber : subscribers ) {
                        if( subscriber.getCreatedDate().getTime() < subscriptionCutoff
                                || subscriber.getLastRefreshDate().getTime() < refreshCutoff ) {
                            GenericFeedSubscription removed = FeedSubscriberServlet.removeSubscriptionEntity( entityManager, subscriber );
                            if( removed != null ) {
                                pushUnsubscriptionNotificationToSubscriber( subscriber, httpClient );
                                removedFeedList.add( removed );
                            }
                        }

                        pushUpdateToSubscriber( subscriber, info, httpClient );
                    }

                    tx.commit();
                    tx = null;
                }
                finally {
                    if( tx != null ) {
                        tx.rollback();
                    }
                }
            }
            finally {
                entityManager.close();
            }

            for( GenericFeedSubscription genSub : removedFeedList ) {
                FeedSubscriberServlet.sendPubSbRemoveRequest( genSub );
            }
        }
        catch( ParserConfigurationException e ) {
            logger.log( Level.SEVERE, "xml parser error", e );
            resp.setStatus( HttpServletResponse.SC_NOT_FOUND );
        }
        catch( SAXException e ) {
            logger.log( Level.WARNING, "error parsing input", e );
            resp.setStatus( HttpServletResponse.SC_NOT_FOUND );
        }
        catch( XPathExpressionException e ) {
            logger.log( Level.SEVERE, "illegal xpath", e );
            resp.setStatus( HttpServletResponse.SC_NOT_FOUND );
        }
    }

    private void pushUpdateToSubscriber( Subscriber subscriber, FeedUpdateInfo info, HttpClient httpClient ) {
        for( FeedUpdateInfoEntry entry : info.getInfoEntryList() ) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add( new BasicNameValuePair( "registration_id", subscriber.getC2dmKey() ) );
            params.add( new BasicNameValuePair( "collapse_key", entry.getId() ) );
            //            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_ID, entry.getId() ) );
            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_TYPE, C2DMSettings.C2DM_TYPE_SEARCH ) );
            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_DEVICE_SUBSCRIPTION_ID, subscriber.getSubscriptionId() ) );
            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_ACTOR, entry.getActor() ) );
            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_ACTOR_ID, entry.getActorId() ) );
            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_UPDATED, String.valueOf( entry.getUpdated() ) ) );
            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_NUM_REPLIES, String.valueOf( entry.getNumReplies() ) ) );
            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_PHOTO_URL, entry.getThumbnailUrl() ) );
            params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_TITLE, parseHtmlAndTruncate( entry.getContent(), 512 ) ) );

            sendC2DMRequestWithParams( httpClient, params );
        }
    }

    private void pushUnsubscriptionNotificationToSubscriber( Subscriber subscriber, HttpClient httpClient ) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add( new BasicNameValuePair( "registration_id", subscriber.getC2dmKey() ) );
        params.add( new BasicNameValuePair( "collapse_key", "unsubscribe:" + subscriber.getId().toString() ) );
        params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_TYPE, C2DMSettings.C2DM_TYPE_UNSUBSCRIPTION_NOTIFICATION ) );
        params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_SEARCH_SUB_MESSAGE_DEVICE_SUBSCRIPTION_ID, subscriber.getSubscriptionId() ) );
        sendC2DMRequestWithParams( httpClient, params );
    }

    private void sendC2DMRequestWithParams( HttpClient httpClient, List<NameValuePair> params ) {
        try {
            HttpPost httpPost = new HttpPost( "https://android.apis.google.com/c2dm/send" );
            httpPost.setHeader( "Authorization", "GoogleLogin auth=" + clientLoginKeyManager.getCachedToken() );
            httpPost.setEntity( new UrlEncodedFormEntity( params ) );

            HttpResponse response = httpClient.execute( httpPost );
            clientLoginKeyManager.processHttpResponse( response );
        }
        catch( ClientProtocolException e ) {
            logger.log( Level.SEVERE, "protocol exception when pushing update", e );
        }
        catch( IOException e ) {
            logger.log( Level.WARNING, "io exception when pushing update", e );
        }
    }

    private static String parseHtmlAndTruncate( String s, int length ) {
        Source source = new Source( s );
        Renderer renderer = new Renderer( source );

        String parsed = renderer.toString();
        return parsed.substring( 0, Math.min( parsed.length(), length ) );
    }

    public static void main( String[] args ) {
        String src = "<html><body>This is<p>A test &amp; blah &lt;newline<br>another line</body></html>";
        String s = parseHtmlAndTruncate( src, 512 );
        System.out.println( "s = " + s );
    }

    private void logRequest( String message, HttpServletRequest request ) {
        StringBuilder buf = new StringBuilder();
        Formatter fmt = new Formatter( buf );
        fmt.format( "Logging request for: %s%n", message );
        fmt.format( "type=%s%n", request.getContentType() );
        fmt.format( "method=%s, localAddr=%s%n", request.getMethod(), request.getLocalAddr() );
        fmt.format( "requestUri=%s%n", request.getRequestURI() );
        fmt.format( "pathInfo=%s%n", request.getPathInfo() );
        fmt.format( "pathTranslated=%s%n", request.getPathTranslated() );
        fmt.format( "queryString=%s%n", request.getQueryString() );

        Enumeration e = request.getParameterNames();
        while( e.hasMoreElements() ) {
            String name = (String)e.nextElement();
            fmt.format( "    %s=%s%n", name, Arrays.toString( request.getParameterValues( name ) ) );
        }
        logger.log( Level.FINE, buf.toString() );
    }

    private void replyToSubscriptionRequest( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
        String topic = request.getParameter( "hub.topic" );
        String challenge = request.getParameter( "hub.challenge" );
//        int leaseSeconds = Integer.parseInt( request.getParameter( "hub.lease_seconds" ) );
        String verifyToken = request.getParameter( "hub.verify_token" );

//        int i = verifyToken.indexOf( "_" );
//        if( i == -1 ) {
//            logger.log( Level.WARNING, "illegal verifyToken format: " + verifyToken );
//            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
//            return;
//        }
//        String tokenValue = verifyToken.substring( 0, i );
//        String tokenId = verifyToken.substring( i + 1 );

        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            GenericFeedSubscription sub = entityManager.find( GenericFeedSubscription.class, topic );
            if( sub == null ) {
                logger.log( Level.WARNING, "subscription not found for token: " + verifyToken );
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            if( !sub.getVerificationId().equals( verifyToken ) ) {
                logger.log( Level.WARNING, "verification id for subscription does not match token: " + verifyToken );
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

//            if( !sub.getFeedUrl().equals( topic ) ) {
//                logger.log( Level.WARNING, "topic " + topic + " does not matchFeedUrl " + sub.getFeedUrl() + " for verifyToken: " + verifyToken );
//                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
//                return;
//            }

            if( sub.isDeleted() ) {
                logger.log( Level.WARNING, "attempt to verify subscription that has already been deleted: " + verifyToken );
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            response.setStatus( HttpServletResponse.SC_OK );

//            response.setContentType( "text/plain" );

            writeChallenge( response, challenge );

            logger.log( Level.FINE, "Successfully responded to subscription request" );

            sub.setVerified( true );
        }
        finally {
            entityManager.close();
        }
    }

    private void replyToUnsubscriptionRequest( HttpServletRequest req, HttpServletResponse resp ) throws ServletException {
        String topic = req.getParameter( "hub.topic" );
        String challenge = req.getParameter( "hub.challenge" );
//        int leaseSeconds = Integer.parseInt( request.getParameter( "hub.lease_seconds" ) );
        String verifyToken = req.getParameter( "hub.verify_token" );

        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            GenericFeedSubscription sub = entityManager.find( GenericFeedSubscription.class, topic );
            if( !verifyGenericFeedSubscription( verifyToken, sub ) ) {
                resp.setStatus( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            logger.log( Level.FINE, "verifying subscription" );

            writeChallenge( resp, challenge );
        }
        finally {
            entityManager.close();
        }
    }

    private boolean verifyGenericFeedSubscription( String verifyToken, GenericFeedSubscription sub ) {
        if( sub == null ) {
            if( FORCE_TOKEN_NAME.equals( verifyToken ) ) {
                return true;
            }

            logger.log( Level.WARNING, "subscription not found for token: " + verifyToken );
            return false;
        }

        if( !sub.getVerificationId().equals( verifyToken ) ) {
            logger.log( Level.WARNING, "verification id for subscription does not match token: " + verifyToken );
            return false;
        }

        if( !sub.isDeleted() ) {
            return false;
        }

        if( !sub.getSubscribers().isEmpty() ) {
            logger.log( Level.WARNING, "got unsubscription request while there are still active subscribers" );
            return false;
        }

        return true;
    }

    private void writeChallenge( HttpServletResponse response, String challenge ) throws ServletException {
        try {
            logger.log( Level.FINE, "returning challenge: " + challenge );

            PrintWriter out = response.getWriter();
            out.print( challenge );
            out.flush();
        }
        catch( IOException e ) {
            throw new ServletException( "exception writing response", e );
        }
    }
}
