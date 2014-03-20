package org.atari.dhs.buzztest.server.tests;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;

import org.atari.dhs.buzztest.server.StandardNamespace;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XpathTest
{
    public static void main( String[] args ) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware( true );

            XPathFactory xPathFactory = XPathFactory.newInstance();

            DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();

            Document doc = docBuilder.parse( new FileInputStream( "bar.xml" ) );

            StandardNamespace replyFeedNamespaceMap = new StandardNamespace();
            replyFeedNamespaceMap.addNamespace( "a", "http://www.w3.org/2005/Atom" );
            replyFeedNamespaceMap.addNamespace( "poco", "http://portablecontacts.net/ns/1.0" );

            XPath xPath = xPathFactory.newXPath();
            xPath.setNamespaceContext( replyFeedNamespaceMap );

            String selfHref = (String)xPath.evaluate( "/a:feed/a:link[@rel='self'][@type='application/atom+xml']/@href", doc, XPathConstants.STRING );
            System.out.println( "selfHref = " + selfHref );

            NodeList nodes = (NodeList)xPath.evaluate( "/a:feed/a:entry", doc, XPathConstants.NODESET );
            int n = nodes.getLength();
            System.out.println( "got " + n + " replies" );
            for( int i = 0 ; i < n ; i++ ) {
                Node node = nodes.item( i );

                String id = (String)xPath.evaluate( "a:id", node, XPathConstants.STRING );
                String published = (String)xPath.evaluate( "a:published", node, XPathConstants.STRING );
                String authorName = (String)xPath.evaluate( "a:author/a:name", node, XPathConstants.STRING );
                String authorUri = (String)xPath.evaluate( "a:author/a:uri", node, XPathConstants.STRING );
                String content = (String)xPath.evaluate( "a:content", node, XPathConstants.STRING );
                String userId = (String)xPath.evaluate( "a:author/poco:id", node, XPathConstants.STRING );

                System.out.println( "incoming reply: id=" + id );
                System.out.println( "published=" + published );
                System.out.println( "authorName=" + authorName );
                System.out.println( "authorUri = " + authorUri );
                System.out.println( "content=" + content );
                System.out.println( "userId = " + userId );
            }
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }
}
