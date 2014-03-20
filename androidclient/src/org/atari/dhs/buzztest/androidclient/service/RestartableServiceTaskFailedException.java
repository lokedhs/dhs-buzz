package org.atari.dhs.buzztest.androidclient.service;

public class RestartableServiceTaskFailedException extends ServiceTaskFailedException
{
    private BundleX restartArgs;

    public RestartableServiceTaskFailedException( String message ) {
        super( message );
    }

    public RestartableServiceTaskFailedException( String message, Throwable cause ) {
        super( message, cause );
    }

    public RestartableServiceTaskFailedException( String message, BundleX restartArgs ) {
        super( message );
        this.restartArgs = restartArgs;
    }

    public RestartableServiceTaskFailedException( String message, Throwable cause, BundleX restartArgs ) {
        super( message, cause );
        this.restartArgs = restartArgs;
    }

    public BundleX getRestartArgs() {
        return restartArgs;
    }
}
