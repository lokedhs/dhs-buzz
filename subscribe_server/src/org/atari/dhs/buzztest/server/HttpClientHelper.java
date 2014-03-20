package org.atari.dhs.buzztest.server;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRoute;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.atari.dhs.buzztest.server.protocol.GAEConnectionManager;

public class HttpClientHelper
{
    private HttpClientHelper() {
        // prevent instantiation
    }

    public static HttpClient createHttpClient() {
        BasicHttpParams httpParams = new BasicHttpParams();

        HttpProtocolParams.setVersion( httpParams, HttpVersion.HTTP_1_1 );

        // No limits
        ConnManagerParams.setMaxTotalConnections( httpParams, Integer.MAX_VALUE );
        ConnManagerParams.setMaxConnectionsPerRoute( httpParams, new ConnPerRoute()
        {
            public int getMaxForRoute( HttpRoute route ) {
                return Integer.MAX_VALUE;
            }
        } );


        HttpClient httpClient = new DefaultHttpClient( new GAEConnectionManager(), httpParams );
        return httpClient;
    }
}
