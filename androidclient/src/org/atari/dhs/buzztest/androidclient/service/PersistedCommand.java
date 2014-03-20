package org.atari.dhs.buzztest.androidclient.service;

import java.io.Serializable;

public class PersistedCommand implements Serializable
{
    private String command;
    private BundleX args;

    public PersistedCommand( String command, BundleX args ) {
        this.command = command;
        this.args = args;
    }

    public String getCommand() {
        return command;
    }

    public BundleX getArgs() {
        return args;
    }
}
