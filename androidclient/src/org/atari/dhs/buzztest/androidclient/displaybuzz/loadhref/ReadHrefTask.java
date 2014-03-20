package org.atari.dhs.buzztest.androidclient.displaybuzz.loadhref;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;
import org.atari.dhs.buzztest.androidclient.Log;
import org.atari.dhs.buzztest.androidclient.tools.asyncsupportactivity.AsyncTaskWrapper;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

class ReadHrefTask extends AsyncTaskWrapper<Uri, Void, ReadHrefTask.Result>
{
    private Pattern hrefPattern;

    ReadHrefTask() {
        // http://buzz.googleapis.com/feeds/115209488640908180409/comments/z123ihewuqurutivc224v5wzmrnxt55lq
        hrefPattern = Pattern.compile( "^http://buzz.googleapis.com/feeds/[^/]+/comments/([^/]+)$" );
    }

    @Override
    protected Result doInBackground( Uri... uris ) {
        try {
            URL url = new URL( uris[0].toString() );
            URLConnection conn = url.openConnection();
            String activityId = processResult( conn );
            if( activityId == null ) {
                return new Result( "Invalid Buzz link", true );
            }

            return new Result( activityId, false );
        }
        catch( IOException e ) {
            Log.e( "IOException when loading buzz", e );
            return new Result( e.getLocalizedMessage(), true );
        }
        catch( ParserException e ) {
            Log.e( "ParserException when loading buzz", e );
            return new Result( e.getLocalizedMessage(), true );
        }
    }

    @Override
    protected void onPostExecute( Result result ) {
        super.onPostExecute( result );

        ((ReadHrefActivity)getUnderlyingActivity()).handleReadHrefResult( result );
    }

    private String processResult( URLConnection conn ) throws IOException, ParserException {
        Parser parser = new Parser( conn );
        NodeList headNodes = parser.parse( new TagNameFilter( "head" ) );
        if( headNodes.size() == 0 ) {
            return null;
        }

        Node head = headNodes.elementAt( 0 );
        NodeList headContent = head.getChildren();
        SimpleNodeIterator iterator = headContent.elements();
        while( iterator.hasMoreNodes() ) {
            Node node = iterator.nextNode();
            if( node instanceof TagNode ) {
                TagNode tagNode = (TagNode)node;
                String url = checkTagNode( tagNode );
                if( url != null ) {
                    return url;
                }
            }
        }

        return null;
    }

    private String checkTagNode( TagNode tagNode ) {
        String tagName = tagNode.getTagName();
        if( tagName == null || !tagName.equals( "LINK" ) ) {
            return null;
        }

        String rel = tagNode.getAttribute( "rel" );
        if( rel == null || !rel.equals( "alternate") ) {
            return null;
        }

        String type = tagNode.getAttribute( "type" );
        if( type == null || !type.equals( "application/atom+xml" ) ) {
            return null;
        }

        String href = tagNode.getAttribute( "href" );
        if( href == null ) {
            return null;
        }

        Matcher matcher = hrefPattern.matcher( href );
        if( !matcher.matches() ) {
            return null;
        }

        String idPart = matcher.group( 1 );
        return "tag:google.com,2010:buzz:" + idPart;
    }

    static class Result
    {
        String errorMessage;
        String activityId;

        Result( String message, boolean isError ) {
            if( isError ) {
                this.errorMessage = message;
            }
            else {
                this.activityId = message;
            }
        }
    }
}
