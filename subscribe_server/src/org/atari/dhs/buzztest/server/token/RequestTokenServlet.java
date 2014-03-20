package org.atari.dhs.buzztest.server.token;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.atari.dhs.buzztest.server.store.C2DMAuthentication;
import org.atari.dhs.buzztest.server.store.EMF;

public class RequestTokenServlet extends HttpServlet
{
    private static final Logger logger = Logger.getLogger( RequestTokenServlet.class.getName() );

    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
        String name = request.getParameter( "username" );
        String password = request.getParameter( "password" );

        if( name == null || name.length() == 0 || password == null || password.length() == 0 ) {
            throw new ServletException( "empty name or password" );
        }

        HttpClient httpClient = HttpClientHelper.createHttpClient();
        HttpPost httpPost = new HttpPost( "https://www.google.com/accounts/ClientLogin" );
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add( new BasicNameValuePair( "accountType", "HOSTED_OR_GOOGLE" ) );
        params.add( new BasicNameValuePair( "Email", C2DMSettings.C2DM_USER ) );
        params.add( new BasicNameValuePair( "Passwd", password ) );
        params.add( new BasicNameValuePair( "service", "ac2dm" ) );
        params.add( new BasicNameValuePair( "source", "lokedhs-dhsbuzz-1" ) );
        httpPost.setEntity( new UrlEncodedFormEntity( params ) );

        HttpResponse result = httpClient.execute( httpPost );
        int statusCode = result.getStatusLine().getStatusCode();
        if( statusCode == HttpServletResponse.SC_OK ) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.getEntity().writeTo( baos );
            String s = new String( baos.toByteArray(), "UTF-8" );
            logger.log( Level.FINE, "got authentication response: " + s );
            for( String part : s.split( "[\n\r]+" ) ) {
                int separatorIndex = part.indexOf( "=" );
                if( separatorIndex != -1 ) {
                    String key = part.substring( 0, separatorIndex );
                    String value = part.substring( separatorIndex + 1 );
                    if( key.equals( "Auth" ) ) {
                        saveKey( name, value );
                        break;
                    }
                }
            }
        }
        else {
            logger.log( Level.SEVERE, "failed authentication: " + statusCode );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            result.getEntity().writeTo( baos );
            String s = new String( baos.toByteArray(), "UTF-8" );
            logger.log( Level.SEVERE, "response: " + s );
        }
    }

    private void saveKey( String name, String value ) {
        EntityManager entityManager = EMF.get().createEntityManager();
        EntityTransaction tx = null;
        try {
            tx = entityManager.getTransaction();
            tx.begin();

            C2DMAuthentication auth;
            //noinspection UnusedCatchParameter
            try {
                auth = entityManager.find( C2DMAuthentication.class, name );
            }
            catch( NoResultException e ) {
                auth = null;
            }

            if( auth == null ) {
                auth = new C2DMAuthentication( name, value );
                entityManager.persist( auth );
            }
            else {
                auth.setToken( value );
            }

            tx.commit();
            tx = null;
        }
        finally {
            if( tx != null ) {
                tx.rollback();
            }
            entityManager.close();
        }
    }

    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {

    }
}
