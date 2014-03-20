package org.atari.dhs.buzztest.androidclient.tools.posteditor;

class UserNameSearchResult
{
    private String name;
    private String email;

    public UserNameSearchResult( String name, String email ) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String toString() {
        return email;
    }
}
