package org.atari.dhs.buzztest.androidclient.service;

import org.atari.dhs.buzztest.androidclient.tools.PendingIntentFactory;

public class ReportableServiceTaskFailedException extends ServiceTaskFailedException
{
    private PendingIntentFactory intent;

    public ReportableServiceTaskFailedException( String message ) {
        super( message );
    }

    public ReportableServiceTaskFailedException( String message, Throwable cause ) {
        super( message, cause );
    }

    public ReportableServiceTaskFailedException( String message, PendingIntentFactory intent ) {
        super( message );
        this.intent = intent;
    }

    public ReportableServiceTaskFailedException( String message, Throwable cause, PendingIntentFactory intent ) {
        super( message, cause );
        this.intent = intent;
    }

    public PendingIntentFactory getIntent() {
        return intent;
    }
}
