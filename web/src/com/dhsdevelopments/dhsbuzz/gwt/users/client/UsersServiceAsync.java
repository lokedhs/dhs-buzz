package com.dhsdevelopments.dhsbuzz.gwt.users.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface UsersServiceAsync
{
    void loadProfiles( AsyncCallback<ProfileList> async );
}
