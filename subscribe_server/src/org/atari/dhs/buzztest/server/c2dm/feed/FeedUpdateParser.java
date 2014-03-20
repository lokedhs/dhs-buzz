package org.atari.dhs.buzztest.server.c2dm.feed;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.intellij.lang.annotations.Language;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.atari.dhs.buzztest.server.StandardNamespace;

public class FeedUpdateParser
{
    private static final Logger logger = Logger.getLogger( FeedUpdateParser.class.getName() );

    private DocumentBuilderFactory documentBuilderFactory;
    private XPathFactory xPathFactory;
    private StandardNamespace replyFeedNamespaceMap;

    public FeedUpdateParser() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware( true );

        replyFeedNamespaceMap = new StandardNamespace();
        replyFeedNamespaceMap.addNamespace( "a", "http://www.w3.org/2005/Atom" );
        replyFeedNamespaceMap.addNamespace( "poco", "http://portablecontacts.net/ns/1.0" );
        replyFeedNamespaceMap.addNamespace( "gd", "http://schemas.google.com/g/2005" );
        replyFeedNamespaceMap.addNamespace( "thr", "http://purl.org/syndication/thread/1.0" );

        xPathFactory = XPathFactory.newInstance();
    }

    public FeedUpdateInfo parse( InputStream in ) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse( in );

        XPaths xPaths = new XPaths();

        XPath xPath = xPathFactory.newXPath();
        xPath.setNamespaceContext( replyFeedNamespaceMap );

        NodeList entries = (NodeList)xPath.evaluate( "/a:feed/a:entry[@gd:kind='buzz#activity']", doc, XPathConstants.NODESET );
        int n = entries.getLength();
        List<FeedUpdateInfoEntry> infoEntryList = new ArrayList<FeedUpdateInfoEntry>( n );
        for( int i = 0 ; i < n ; i++ ) {
            infoEntryList.add( xPaths.parseEntry( entries.item( i ) ) );
        }
        return new FeedUpdateInfo( infoEntryList );
    }

    public static void main( String[] args ) {
        try {
            FeedUpdateParser p = new FeedUpdateParser();

            FileInputStream in = new FileInputStream( "foo.xml" );
            FeedUpdateInfo info = p.parse( in );
            System.out.println( "result = " + info );
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    private class XPaths
    {
        /*
        String id;
        String actor;
        int numReplies;
        long updated;
        String title;
        String thumbnailUrl;

            <link thr:count="1"
                  href="https://www.googleapis.com/buzz/v1/activities/117768027792570348635/@self/B:z12tw1minqilg1du323rwrsg0x3rwdxa004/@comments?alt=atom"
                  type="application/atom+xml" thr:updated="2010-12-14T15:23:44.633Z" rel="replies"/>

        */
        private XPathExpression id;
        private XPathExpression actor;
        private XPathExpression actorId;
        private XPathExpression numReplies;
        private XPathExpression updated;
        private XPathExpression content;
        private XPathExpression thumbnailUrl;

        XPaths() throws XPathExpressionException {
            id = makeXPath( "a:id" );
            actor = makeXPath( "a:author/a:name" );
            actorId = makeXPath( "a:author/poco:id" );
            numReplies = makeXPath( "a:link[@rel='replies']/@thr:count" );
            updated = makeXPath( "a:updated" );
            content = makeXPath( "a:content" );
            thumbnailUrl = makeXPath( "a:author/poco:photoUrl" );
        }

        private XPathExpression makeXPath( @Language("XPath") String expression ) throws XPathExpressionException {
            XPath xPath = xPathFactory.newXPath();
            xPath.setNamespaceContext( replyFeedNamespaceMap );
            return xPath.compile( expression );
        }

        public FeedUpdateInfoEntry parseEntry( Node item ) throws XPathExpressionException {
            String idString = id.evaluate( item );
            String actorString = actor.evaluate( item );
            String actorIdString = actorId.evaluate( item );
            String numRepliesString = numReplies.evaluate( item );
            String updatedString = updated.evaluate( item );
            String contentString = content.evaluate( item );
            String thumbnailUrlString = thumbnailUrl.evaluate( item );

            int numRepliesInt = 0;
            if( numRepliesString != null && !numRepliesString.equals( "" ) ) {
                numRepliesInt = Integer.parseInt( numRepliesString );
            }

            SimpleDateFormat inputFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
            inputFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
            long updatedLong;
            try {
                updatedLong = inputFormat.parse( updatedString ).getTime();
            }
            catch( ParseException e ) {
                logger.log( Level.SEVERE, "can't parse date: " + updatedString + " for activity: " + idString, e );
                updatedLong = 0;
            }

            return new FeedUpdateInfoEntry( idString, actorString, actorIdString, numRepliesInt, updatedLong, contentString, thumbnailUrlString );
        }
    }
}
