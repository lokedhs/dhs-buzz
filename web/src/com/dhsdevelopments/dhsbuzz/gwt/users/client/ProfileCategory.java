package com.dhsdevelopments.dhsbuzz.gwt.users.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProfileCategory implements Serializable
{
    private String name;
    private List<Profile> profiles;

    public ProfileCategory() {
        // Required for GWT
    }

    public String getName() {
        return name;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public static ProfileCategory makeDemoCategory( String name, int numEntries ) {
        ProfileCategory category = new ProfileCategory();
        category.name = name;
        category.profiles = new ArrayList<Profile>();
        for( int i = 0 ; i < numEntries ; i++ ) {
            category.profiles.add( new Profile( "name" + i, "profileid:" + i ) );
        }
        return category;
    }
}
