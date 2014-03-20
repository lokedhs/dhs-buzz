package org.atari.dhs.buzztest.server.protocol;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpParams;

@SuppressWarnings({ "AppEngineForbiddenCode" })
public class GAEConnectionManager implements ClientConnectionManager
{

    public GAEConnectionManager()
    {
        SocketFactory noSocketFactory = new SocketFactory()
        {
            public Socket connectSocket( Socket sock, String host, int port,
                                         InetAddress localAddress, int localPort,
                                         HttpParams params )
            {
                return null;
            }

            public Socket createSocket()
            {
                return null;
            }

            public boolean isSecure( Socket s )
            {
                return false;
            }
        };

        schemeRegistry = new SchemeRegistry();
        schemeRegistry.register( new Scheme( "http", noSocketFactory, 80 ) );
        schemeRegistry.register( new Scheme( "https", noSocketFactory, 443 ) );
    }


    @Override
    public SchemeRegistry getSchemeRegistry()
    {
        return schemeRegistry;
    }

    @Override
    public ClientConnectionRequest requestConnection( final HttpRoute route,
                                                      final Object state )
    {
        return new ClientConnectionRequest()
        {
            public void abortRequest()
            {
                // Nothing to do
            }

            public ManagedClientConnection getConnection( long timeout, TimeUnit timeUnit )
            {
                return GAEConnectionManager.this.getConnection( route, state );
            }
        };
    }

    @Override
    public void releaseConnection( ManagedClientConnection conn,
                                   long validDuration, TimeUnit timeUnit )
    {
    }

    @Override
    public void closeIdleConnections( long time, TimeUnit timeUnit )
    {
    }

    @Override
    public void closeExpiredConnections()
    {
    }

    @Override
    public void shutdown()
    {
    }

    private ManagedClientConnection getConnection( HttpRoute route, Object state )
    {
        return new GAEClientConnection( this, route, state );
    }

    private SchemeRegistry schemeRegistry;
}
