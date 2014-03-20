package org.atari.dhs.buzztest.server.c2dm.feed;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
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
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.urlfetch.HTTPRequest;
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
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.atari.dhs.buzztest.common.C2DMSettings;
import org.atari.dhs.buzztest.server.BuzzConfig;
import org.atari.dhs.buzztest.server.HttpClientHelper;
import org.atari.dhs.buzztest.server.store.EMF;
import org.atari.dhs.buzztest.server.store.feed.GenericFeedSubscription;
import org.atari.dhs.buzztest.server.store.feed.Subscriber;
import org.atari.dhs.buzztest.server.store.feed.SubscriptionType;

public class FeedSubscriberServlet extends HttpServlet
{
    private static final Logger logger = Logger.getLogger( FeedSubscriberServlet.class.getName() );

    public static final String PATH_ID_PARAM_NAME = "pathId";

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

        if( C2DMSettings.C2DM_COMMAND_VALUE_SUBSCRIBE.equals( command ) ) {
            subscribeToSearch( request );
        }
        else if( C2DMSettings.C2DM_COMMAND_VALUE_UNSUBSCRIBE.equals( command ) ) {
            unsubscribeFromSearch( request );
        }
        else if( C2DMSettings.C2DM_COMMAND_VALUE_REFRESH.equals( command ) ) {
            refreshSearch( request );
        }
        else {
            response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
        }
    }

    private void subscribeToSearch( HttpServletRequest request ) throws ServletException {
        try {
            String searchTarget = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_SEARCH_TARGET ) );
            String c2dmKey = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_KEY ) );
            String username = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_USER_NAME ) );
            String userId = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_USER_ID ) );
            String deviceSubscriptionId = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_DEVICE_SUBSCRIPTION_ID ) );

            checkSearchTargetValid( searchTarget );

            URL url = new URL( String.format( "https://www.googleapis.com/buzz/v1/activities/track?key=%s&q=%s",
                                              BuzzConfig.SERVER_BUZZ_API_KEY,
                                              URLEncoder.encode( searchTarget, "UTF-8" ) ) );

            URLFetchService urlFetch = URLFetchServiceFactory.getURLFetchService();
            HTTPRequest httpRequest = new HTTPRequest( url );
            httpRequest.getFetchOptions().setDeadline( 20.0 );
            HTTPResponse response = urlFetch.fetch( httpRequest );

            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse( new ByteArrayInputStream( response.getContent() ) );

            XPath xPath = xPathFactory.newXPath();
            String hubUrl = (String)xPath.evaluate( "/feed/link[@rel='hub']/@href", doc, XPathConstants.STRING );
            if( hubUrl == null ) {
                throw new ServletException( "can't find hub url in document" );
            }

            String selfLink = (String)xPath.evaluate( "/feed/link[@rel='self'][@type='application/atom+xml']/@href", doc, XPathConstants.STRING );
            if( selfLink == null ) {
                throw new ServletException( "no self link in document" );
            }

            createSubscriptionEntryInDatastore( selfLink, searchTarget, c2dmKey, username, userId, hubUrl, deviceSubscriptionId );
        }
        catch( IOException e ) {
            throw new ServletException( e );
        }
        catch( ParserConfigurationException e ) {
            throw new ServletException( "Error parsing buzz result", e );
        }
        catch( SAXException e ) {
            throw new ServletException( "Error parsing buzz result", e );
        }
        catch( XPathExpressionException e ) {
            throw new ServletException( "Error parsing buzz result", e );
        }
    }

    /**
     * Check to make sure that the search expression is valid. Currently this means that at least
     * one of the words start with a #.
     *
     * @param searchTarget the search expression
     * @throws ServletException if the search expression is not valid
     */
    private void checkSearchTargetValid( String searchTarget ) throws ServletException {
        // TODO: all searches are now valid
//        String[] parts = searchTarget.split( " " );
//        boolean found = false;
//        for( String s : parts ) {
//            if( s.length() >= 2 && s.charAt( 0 ) == '#' ) {
//                found = true;
//                break;
//            }
//        }
//
//        if( !found ) {
//            throw new ServletException( "illegal search expression" );
//        }
    }

    private void createSubscriptionEntryInDatastore( String feedUrl, String expression, String c2dmKey, String username, String userId,
                                                     String hubUrl, String deviceSubscriptionId ) throws ServletException {
        Date now = new Date();

        boolean startSubscription = false;
        GenericFeedSubscription sub;

        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();
            try {
                sub = entityManager.find( GenericFeedSubscription.class, feedUrl );
                if( sub == null ) {
                    StringBuilder buf = new StringBuilder();
                    for( int i = 0 ; i < 30 ; i++ ) {
                        buf.append( (char)((Math.random() * ('z' - 'a' + 1)) + 'a') );
                    }

                    sub = new GenericFeedSubscription( feedUrl, SubscriptionType.SEARCH, hubUrl, expression, buf.toString(), now );
                    entityManager.persist( sub );

                    startSubscription = true;
                }
                else {
                    if( sub.isDeleted() ) {
                        sub.setDeleted( false );
                        sub.setVerified( false );
                        startSubscription = true;
                    }
                }

                Subscriber subscriber = new Subscriber( c2dmKey, sub, username, userId, now, deviceSubscriptionId );
                sub.getSubscribers().add( subscriber );
                entityManager.persist( subscriber );

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

        if( startSubscription ) {
            sendPubSubRequest( hubUrl, feedUrl, sub.getVerificationId(), true );
        }
    }

    private void unsubscribeFromSearch( HttpServletRequest request ) throws ServletException {
        String subscriptionId = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_DEVICE_SUBSCRIPTION_ID ) );
        String expression = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_SEARCH_TARGET ) );
        String c2dmKey = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_KEY ) );

        GenericFeedSubscription willUnsubscribe;

        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();
            try {
//                Query query = entityManager.createNamedQuery( "findGenericFeedSubscriptionByExpression" );
//                query.setParameter( "expression", expression );
//                GenericFeedSubscription gSub = (GenericFeedSubscription)query.getSingleResult();
//                if( gSub == null ) {
//                    logger.log( Level.SEVERE, "attempt to refresh subscription for removed feed, should resubscribe: '" + expression + "'" );
//                    return;
//                }

                Query query = entityManager.createNamedQuery( "findSubscriberBySubscriptionIdAndC2DMKey" );
                query.setParameter( "subscriptionId", subscriptionId );
                query.setParameter( "c2dmKey", c2dmKey );
                Subscriber sub = (Subscriber)query.getSingleResult();
                if( sub == null ) {
                    logger.log( Level.FINE, "attempt to unsubscribe from removed feed. expression='" + expression + "', c2dmKey='" + c2dmKey + "'" );
                    return;
                }
                if( !sub.getParent().getExpression().equals( expression ) ) {
                    logger.log( Level.WARNING, "expression does not match subscription id. c2DMKey=" + c2dmKey + ", id=" + subscriptionId + ", expression=" + expression );
                    // TODO: what is the right thing to do here? For now, just remove the subscription.
                }

                willUnsubscribe = removeSubscriptionEntity( entityManager, sub );

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

        if( willUnsubscribe != null ) {
            sendPubSbRemoveRequest( willUnsubscribe );
        }
    }

    static GenericFeedSubscription removeSubscriptionEntity( EntityManager entityManager, Subscriber sub ) {
        GenericFeedSubscription willUnsubscribe;
        GenericFeedSubscription parent = sub.getParent();
        parent.getSubscribers().remove( sub );
        entityManager.remove( sub );

        if( parent.getSubscribers().isEmpty() ) {
            willUnsubscribe = parent;
            parent.setDeleted( true );
            parent.setVerified( false );
        }
        else {
            willUnsubscribe = null;
        }

        return willUnsubscribe;
    }

    private void refreshSearch( HttpServletRequest request ) throws ServletException {
        String expression = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_SEARCH_TARGET ) );
        String c2dmKey = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_KEY ) );
        String subscriptionId = checkAndTrim( request.getParameter( C2DMSettings.C2DM_PARAM_REQUEST_DEVICE_SUBSCRIPTION_ID ) );

        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();
            try {
//                Query query = entityManager.createNamedQuery( "findGenericFeedSubscriptionByExpression" );
//                query.setParameter( "expression", expression );
//                GenericFeedSubscription gSub = (GenericFeedSubscription)query.getSingleResult();
//                if( gSub == null ) {
//                    logger.log( Level.SEVERE, "attempt to refresh subscription for removed feed, should resubscribe: '" + expression + "'" );
//                    return;
//                }

                Query query = entityManager.createNamedQuery( "findSubscriberBySubscriptionIdAndC2DMKey" );
                query.setParameter( "subscriptionId", subscriptionId );
                query.setParameter( "c2dmKey", c2dmKey );
                Subscriber sub = (Subscriber)query.getSingleResult();
                if( sub == null ) {
                    logger.log( Level.SEVERE, "attempt to refresh subscription for removed feed, should resubscribe: expression='" + expression + "', c2dmKey='" + c2dmKey + "'" );
                    return;
                }
                if( !sub.getParent().getExpression().equals( expression ) ) {
                    logger.log( Level.SEVERE, "attempt to unsubscribe with mismatched expression. sub=" + sub + ", expression=" + expression );
                }

                logger.log( Level.FINE, "refreshing date for feed: " + sub );

                sub.setLastRefreshDate( new Date() );

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
    }

    static void sendPubSbRemoveRequest( GenericFeedSubscription willUnsubscribe ) throws ServletException {
        sendPubSubRequest( willUnsubscribe.getHubUrl(), willUnsubscribe.getFeedUrl(), willUnsubscribe.getVerificationId(), false );
    }

    static void sendPubSubRequest( String hubUrl, String topicUrl, String verificationId, boolean subscribe ) throws ServletException {
        logger.log( Level.FINE, "requesting subscription for hubUrl=" + hubUrl + ", followUrl=" + topicUrl + ", verificationId=" + verificationId + ", subscribe=" + subscribe );

        try {
            HttpClient httpClient = HttpClientHelper.createHttpClient();

            HttpPost httpPost = new HttpPost( hubUrl );
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add( new BasicNameValuePair( "hub.callback", "https://lokemaptest.appspot.com/FeedMessageAndroid?" + PATH_ID_PARAM_NAME + "=" + URLEncoder.encode( topicUrl, "UTF-8" ) ) );
            params.add( new BasicNameValuePair( "hub.mode", subscribe ? "subscribe" : "unsubscribe" ) );
            params.add( new BasicNameValuePair( "hub.topic", topicUrl ) );
            params.add( new BasicNameValuePair( "hub.verify", "async" ) );
            params.add( new BasicNameValuePair( "hub.verify_token", verificationId ) );
            httpPost.setEntity( new UrlEncodedFormEntity( params ) );

            HttpResponse httpResponse = httpClient.execute( httpPost );
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if( statusCode != HttpServletResponse.SC_OK && statusCode != HttpServletResponse.SC_ACCEPTED && statusCode != HttpServletResponse.SC_NO_CONTENT ) {
                logEntity( httpResponse.getEntity() );
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

    private static void logEntity( HttpEntity entity ) throws IOException {
        if( entity != null ) {
            InputStream content = entity.getContent();
            if( content != null ) {
                logger.log( Level.WARNING, "logging entity" );
                BufferedReader in = new BufferedReader( new InputStreamReader( content, "UTF-8" ) );
                String s;
                while( (s = in.readLine()) != null ) {
                    logger.log( Level.WARNING, "errorRow:" + s );
                }
            }
        }
    }

    private static String checkAndTrim( String s ) throws ServletException {
        if( s == null ) {
            throw new ServletException( "missing parameter" );
        }

        s = s.trim();
        if( s.equals( "" ) ) {
            throw new ServletException( "empty parameter" );
        }

        return s;
    }
}
