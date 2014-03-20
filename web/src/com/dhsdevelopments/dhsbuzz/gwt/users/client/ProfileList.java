package com.dhsdevelopments.dhsbuzz.gwt.users.client;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProfileList implements Serializable
{
    private List<ProfileCategory> categories;

    public ProfileList() {
        // Required for GWT
    }

    public List<ProfileCategory> getCategories() {
        return categories;
    }

    public static ProfileList makeDemoList() {
        ProfileList l = new ProfileList();
        l.categories = new ArrayList<ProfileCategory>();
        l.categories.add( ProfileCategory.makeDemoCategory( "foo", 10 ) );
        l.categories.add( ProfileCategory.makeDemoCategory( "bar", 4 ) );
        return l;
    }
}
