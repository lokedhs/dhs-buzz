package org.atari.dhs.buzztest.androidclient.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BundleX implements Serializable
{
    private Map<String,Object> objects;

    public BundleX() {
        objects = new HashMap<String, Object>();
    }

    public BundleX( BundleX args ) {
        objects = new HashMap<String, Object>( args.objects );
    }

    public String getStringWithCheck( String key ) {
        String value = (String)objects.get( key );
        if( value == null ) {
            throw new IllegalStateException( "value for key " + key + " missing" );
        }
        return value;
    }

    public String getString( String key ) {
        return (String)objects.get( key );
    }

    public void putString( String key, String value ) {
        objects.put( key, value );
    }

    public String[] getStringArray( String key ) {
        return (String[])objects.get( key );
    }

    public void putStringArray( String key, String[] value ) {
        objects.put( key, value );
    }

    public boolean getBooleanWithCheck( String key ) {
        Boolean value = (Boolean)objects.get( key );
        if( value == null ) {
            throw new IllegalStateException( "object doesn't contain key: " + key );
        }
        return value;
    }

    public boolean getBoolean( String key, boolean defaultValue ) {
        Boolean value = (Boolean)objects.get( key );
        if( value == null ) {
            return defaultValue;
        }
        else {
            return value;
        }
    }

    public void putBoolean( String key, boolean value ) {
        objects.put( key, value );
    }

    public long getLongWithCheck( String key ) {
        Long value = (Long)objects.get( key );
        if( value == null ) {
            throw new IllegalStateException( "value for key " + key + " missing" );
        }
        return value;
    }

    public void putLong( String key, long value ) {
        objects.put( key, value );
    }
}
