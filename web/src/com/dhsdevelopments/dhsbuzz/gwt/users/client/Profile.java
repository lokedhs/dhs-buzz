package com.dhsdevelopments.dhsbuzz.gwt.users.client;

import java.io.Serializable;

public class Profile implements Serializable
{
    private String name;
    private String id;

    public Profile() {
        // Required for GWT
    }

    public Profile( String id, String name ) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
