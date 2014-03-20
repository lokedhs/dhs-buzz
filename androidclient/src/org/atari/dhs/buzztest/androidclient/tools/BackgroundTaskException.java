package org.atari.dhs.buzztest.androidclient.tools;

public class BackgroundTaskException extends Exception
{
    public BackgroundTaskException()
    {
        super();
    }

    public BackgroundTaskException( String message )
    {
        super( message );
    }

    public BackgroundTaskException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public BackgroundTaskException( Throwable cause )
    {
        super( cause );
    }
}
