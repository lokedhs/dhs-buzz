package org.atari.dhs.buzztest.server.c2dm;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.atari.dhs.buzztest.common.C2DMSettings;
import org.atari.dhs.buzztest.server.store.C2DMAuthentication;
import org.atari.dhs.buzztest.server.store.EMF;

public class ClientLoginKeyManager
{
    private static final Logger logger = Logger.getLogger( ClientLoginKeyManager.class.getName() );

    private static final String MEMCACHE_KEY = "c2dmtoken";

    private Cache cache;

    public ClientLoginKeyManager() throws ServletException {
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            cache = cacheFactory.createCache( Collections.emptyMap() );
        }
        catch( CacheException e ) {
            throw new ServletException( "failed to init cache", e );
        }
    }

    public synchronized String getCachedToken() {
        String token = (String)cache.get( MEMCACHE_KEY );
        if( token == null ) {
            token = loadTokenFromDatabase();
            cache.put( MEMCACHE_KEY, token );
        }
        return token;
    }

    private String loadTokenFromDatabase() {
        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            C2DMAuthentication auth = entityManager.find( C2DMAuthentication.class, C2DMSettings.C2DM_USER );
            return auth.getToken();
        }
        finally {
            entityManager.close();
        }
    }

    public synchronized void saveUpdatedToken( String value ) {
        EntityManager entityManager = EMF.get().createEntityManager();
        try {
            cache.put( MEMCACHE_KEY, value );

            C2DMAuthentication auth = entityManager.find( C2DMAuthentication.class, C2DMSettings.C2DM_USER );
            if( auth == null ) {
                logger.log( Level.SEVERE, "c2dm authentication data not found when trying to update new authentication" );
            }
            else {
                auth.setToken( value );
                auth.setLastC2DMUpdateTime( new Date() );
            }
        }
        finally {
            entityManager.close();
        }
    }

    public void processHttpResponse( HttpResponse response ) throws IOException {
        if( response.getStatusLine().getStatusCode() == HttpServletResponse.SC_OK ) {
            Header[] updateClientAuthHeaders = response.getHeaders( "Update-Client-Auth" );
            if( updateClientAuthHeaders != null && updateClientAuthHeaders.length > 0 ) {
                Header updatedToken = updateClientAuthHeaders[0];
                saveUpdatedToken( updatedToken.getValue() );
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            response.getEntity().writeTo( baos );

            String result = new String( baos.toByteArray(), "UTF-8" );
            logger.log( Level.FINE, "result=" + result );
        }
    }
}
