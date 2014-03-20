package org.atari.dhs.buzztest.server;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gdata.util.common.base.Preconditions;

public class StandardNamespace implements NamespaceContext
{
    private Map<String, String> namespaceMap;

    public StandardNamespace() {
        namespaceMap = new HashMap<String, String>();
        namespaceMap.put( "xml", XMLConstants.XML_NS_URI );
    }

    public void addNamespace( String name, String url ) {
        namespaceMap.put( name, url );
    }

    @Override
    public String getNamespaceURI( String s ) {
        Preconditions.checkNotNull( s, "namespace prefix can't be null" );

        String url = namespaceMap.get( s );
        if( url == null ) {
            return XMLConstants.NULL_NS_URI;
        }

        return url;
    }

    @Override
    public String getPrefix( String s ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator getPrefixes( String s ) {
        throw new UnsupportedOperationException();
    }
}
