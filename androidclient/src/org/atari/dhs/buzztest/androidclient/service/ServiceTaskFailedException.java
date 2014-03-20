package org.atari.dhs.buzztest.androidclient.service;

public class ServiceTaskFailedException extends Exception
{
    public ServiceTaskFailedException() {
    }

    public ServiceTaskFailedException( String message ) {
        super( message );
    }

    public ServiceTaskFailedException( String message, Throwable cause ) {
        super( message, cause );
    }

    public ServiceTaskFailedException( Throwable cause ) {
        super( cause );
    }
}
