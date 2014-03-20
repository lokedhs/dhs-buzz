package org.atari.dhs.buzztest.server.c2dm;

import javax.persistence.EntityManager;
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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.atari.dhs.buzztest.common.C2DMSettings;
import org.atari.dhs.buzztest.server.HttpClientHelper;
import org.atari.dhs.buzztest.server.StandardNamespace;
import org.atari.dhs.buzztest.server.store.EMF;
import org.atari.dhs.buzztest.server.store.SubscribedFeed;
import org.atari.dhs.buzztest.server.store.UnsubscriptionRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PushMessageAndroidServlet extends HttpServlet
{
    private static final Logger logger = Logger.getLogger( PushMessageAndroidServlet.class.getName() );

    private DocumentBuilderFactory documentBuilderFactory;
    private XPathFactory xPathFactory;
    private ClientLoginKeyManager clientLoginKeyManager;

    private StandardNamespace replyFeedNamespaceMap;

    @Override
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );

        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware( true );

        xPathFactory = XPathFactory.newInstance();
        replyFeedNamespaceMap = new StandardNamespace();
        replyFeedNamespaceMap.addNamespace( "a", "http://www.w3.org/2005/Atom" );
        replyFeedNamespaceMap.addNamespace( "poco", "http://portablecontacts.net/ns/1.0" );

        clientLoginKeyManager = new ClientLoginKeyManager();
    }

    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        logRequest( "post", request );

        try {
            parseIncomingPost( request, response );
        }
        catch( Exception e ) {
            logger.log( Level.SEVERE, "failed, but ignored", e );
        }
    }

    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        logRequest( "get", request );

        String mode = request.getParameter( "hub.mode" );
        if( "subscribe".equals( mode ) ) {
            replyToSubscriptionRequest( request, response );
        }
        else if( "unsubscribe".equals( mode ) ) {
            replyToUnsubscriptionRequest( request, response );
        }
        else {
            logger.warning( "unknown mode: " + mode );
        }
    }

    private void replyToSubscriptionRequest( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        String topic = request.getParameter( "hub.topic" );
        String challenge = request.getParameter( "hub.challenge" );
        int leaseSeconds = Integer.parseInt( request.getParameter( "hub.lease_seconds" ) );
        String verifyToken = request.getParameter( "hub.verify_token" );

        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            Query query = entityManager.createNamedQuery( "findSubscribedFeedByVerifyTokenAndVerified" );
            query.setParameter( "verificationId", verifyToken );
            query.setParameter( "verified", false );
            SubscribedFeed subscriptionRequest;
            //noinspection UnusedCatchParameter
            try {
                subscriptionRequest = (SubscribedFeed)query.getSingleResult();
            }
            catch( NoResultException e ) {
                subscriptionRequest = null;
            }
            if( subscriptionRequest == null || subscriptionRequest.isVerified() ) {
                logger.log( Level.WARNING, "No active subscription request found" );
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                return;
            }
            logger.info( "got request. id=" + subscriptionRequest.getId() );

            long now = System.currentTimeMillis();
            if( subscriptionRequest.getCreatedDate().getTime() < now - (10 * 60 * 1000) ) {
                logger.log( Level.WARNING, "Subscriptions request too late: " + subscriptionRequest.getCreatedDate() + ", " + new Date( now ) );
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            response.setStatus( HttpServletResponse.SC_OK );

            logger.log( Level.INFO, "Sending response challenge" );

            PrintWriter out = response.getWriter();
            out.print( challenge );
            out.flush();

            logger.log( Level.INFO, "Successfully responded to subscription request" );

            subscriptionRequest.setVerified( true );
//            subscriptionRequest.setVerificationId( "" );
        }
        finally {
            entityManager.close();
        }
    }

    private void parseIncomingPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException {
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse( request.getInputStream() );

            XPath xPath = xPathFactory.newXPath();
            xPath.setNamespaceContext( replyFeedNamespaceMap );

            String selfHref = (String)xPath.evaluate( "/a:feed/a:link[@rel='self'][@type='application/atom+xml']/@href", doc, XPathConstants.STRING );
            if( selfHref == null || selfHref.equals( "" ) ) {
                logger.log( Level.WARNING, "no self href in feed" );
                return;
            }

            // The self link as posted by the feed contains ?alt=atom, just remove it for the purposes
            // of matching the feed against the registered listeners
            String cleanUrlName;
            int charIndex = selfHref.indexOf( "?" );
            if( charIndex == -1 ) {
                cleanUrlName = selfHref;
            }
            else {
                cleanUrlName = selfHref.substring( 0, charIndex );
            }

            EntityManager entityManager = EMF.get().createEntityManager();
            try {
                Query query = entityManager.createNamedQuery( "findSubscribedFeedByFeed" );
                query.setParameter( "commentsUrl", cleanUrlName );
                List subscribers = query.getResultList();
                logger.fine( "subscribers. count=" + subscribers.size() + ", href=" + selfHref );
                if( subscribers.isEmpty() ) {
                    throw new ServletException( "no active subscribers for url: " + cleanUrlName );
                }
                else {
                    NodeList nodes = (NodeList)xPath.evaluate( "/a:feed/a:entry", doc, XPathConstants.NODESET );
                    int n = nodes.getLength();
                    logger.fine( "got " + n + " replies" );
                    for( int i = 0 ; i < n ; i++ ) {
                        Node node = nodes.item( i );

                        String id = (String)xPath.evaluate( "a:id", node, XPathConstants.STRING );
                        String published = (String)xPath.evaluate( "a:published", node, XPathConstants.STRING );
                        String authorName = (String)xPath.evaluate( "a:author/a:name", node, XPathConstants.STRING );
                        String authorUri = (String)xPath.evaluate( "a:author/a:uri", node, XPathConstants.STRING );
                        String content = (String)xPath.evaluate( "a:content", node, XPathConstants.STRING );
                        String userId = (String)xPath.evaluate( "a:author/poco:id", node, XPathConstants.STRING );

                        logger.fine( "incoming reply: id=" + id + ", published=" + published + ", author=" + authorName + ", userId=" + userId + ", content=" + content );

                        // there is a potential risk that there are multiple registrations for
                        // a device/feed pair.
                        Map<ActivityIdPair, SubscribedFeed> toSend = new HashMap<ActivityIdPair, SubscribedFeed>();
                        for( Object o : subscribers ) {
                            SubscribedFeed subscribedFeed = (SubscribedFeed)o;
                            toSend.put( new ActivityIdPair( subscribedFeed.getActivityId(), subscribedFeed.getC2dmKey() ), subscribedFeed );
                        }

                        for( SubscribedFeed subscribedFeed : toSend.values() ) {
                            if( userId == null || userId.length() == 0 || !userId.equals( subscribedFeed.getUserId() ) ) {
                                pushMessageToFeed( subscribedFeed, subscribedFeed.getActivityId(), published, authorName, authorUri, content );
                            }
                        }
                    }
                }
            }
            finally {
                entityManager.close();
            }
        }
        catch( ParserConfigurationException e ) {
            throw new ServletException( "exception when parsing result", e );
        }
        catch( SAXException e ) {
            throw new ServletException( "exception when parsing result", e );
        }
        catch( IOException e ) {
            throw new ServletException( "exception when parsing result", e );
        }
        catch( XPathExpressionException e ) {
            throw new ServletException( "exception when parsing result", e );
        }
    }

    private void pushMessageToFeed( SubscribedFeed subscribedFeed, String id, String published, String authorName, String authorUri, String content ) throws IOException, ServletException {
        HttpClient httpClient = HttpClientHelper.createHttpClient();

        HttpPost httpPost = new HttpPost( "https://android.apis.google.com/c2dm/send" );
        httpPost.setHeader( "Authorization", "GoogleLogin auth=" + clientLoginKeyManager.getCachedToken() );
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add( new BasicNameValuePair( "registration_id", subscribedFeed.getC2dmKey() ) );
        params.add( new BasicNameValuePair( "collapse_key", id ) );
        params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_TYPE, C2DMSettings.C2DM_TYPE_ACTIVITY ) );
        params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_ACTIVITY_SUB_MESSAGE_SENDER, authorName ) );
        params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_ACTIVITY_SUB_MESSAGE_PUBLISHED, published ) );
        params.add( new BasicNameValuePair( "data." + C2DMSettings.C2DM_FIELD_ACTIVITY_SUB_MESSAGE_ACTIVITY_ID, subscribedFeed.getActivityId() ) );
        httpPost.setEntity( new UrlEncodedFormEntity( params ) );

        HttpResponse response = httpClient.execute( httpPost );
//        if( response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK ) {
//            processPushResponse( response );
//        }
        clientLoginKeyManager.processHttpResponse( response );
    }

//    private void processPushResponse( HttpResponse response ) throws IOException {
//        Header[] updateClientAuthHeaders = response.getHeaders( "Update-Client-Auth" );
//        if( updateClientAuthHeaders != null && updateClientAuthHeaders.length > 0 ) {
//            Header updatedToken = updateClientAuthHeaders[0];
//            clientLoginKeyManager.saveUpdatedToken( updatedToken.getValue() );
//        }
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        response.getEntity().writeTo( baos );
//
//        String result = new String( baos.toByteArray(), "UTF-8" );
//        logger.log( Level.FINE, "result=" + result );
//    }

    private void replyToUnsubscriptionRequest( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        String topic = request.getParameter( "hub.topic" );
        String challenge = request.getParameter( "hub.challenge" );
        int leaseSeconds = Integer.parseInt( request.getParameter( "hub.lease_seconds" ) );
        String verifyToken = request.getParameter( "hub.verify_token" );

        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            Query query = entityManager.createNamedQuery( "findSubscribedFeedByFeed" );
            query.setParameter( "commentsUrl", topic );
            List results = query.getResultList();
            if( !results.isEmpty() ) {
                logger.log( Level.WARNING, "unsubscription challenge when there are still registered subscribers" );
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
                return;
            }

            UnsubscriptionRequest unsubscriptionRequest = entityManager.find( UnsubscriptionRequest.class, topic );
            if( unsubscriptionRequest == null ) {
                logger.log( Level.WARNING, "unsubscription request missing" );
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }
            else if( !verifyToken.equals( unsubscriptionRequest.getVerificationId() ) ) {
                logger.log( Level.WARNING, "verification id mismatch" );
                response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            }
            else {
                response.setStatus( HttpServletResponse.SC_OK );

                logger.log( Level.INFO, "Sending unsubscription response challenge" );

                PrintWriter out = response.getWriter();
                out.print( challenge );
                out.flush();

                logger.log( Level.INFO, "Successfully responded to unsubscription request" );

                unsubscriptionRequest.setVerified( true );
            }
        }
        finally {
            entityManager.close();
        }
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

        Enumeration e = request.getParameterNames();
        while( e.hasMoreElements() ) {
            String name = (String)e.nextElement();
            fmt.format( "    %s=%s%n", name, Arrays.toString( request.getParameterValues( name ) ) );
        }
        logger.log( Level.FINE, buf.toString() );
    }

    private class ActivityIdPair
    {
        private String activityId;
        private String c2dmKey;

        public ActivityIdPair( String activityId, String c2dmKey ) {
            this.activityId = activityId;
            this.c2dmKey = c2dmKey;
        }

        @SuppressWarnings( { "RedundantIfStatement" })
        @Override
        public boolean equals( Object o ) {
            if( this == o ) {
                return true;
            }
            if( o == null || getClass() != o.getClass() ) {
                return false;
            }

            ActivityIdPair that = (ActivityIdPair)o;

            if( activityId != null ? !activityId.equals( that.activityId ) : that.activityId != null ) {
                return false;
            }
            if( c2dmKey != null ? !c2dmKey.equals( that.c2dmKey ) : that.c2dmKey != null ) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = activityId != null ? activityId.hashCode() : 0;
            result = 31 * result + (c2dmKey != null ? c2dmKey.hashCode() : 0);
            return result;
        }
    }
}
