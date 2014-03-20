package com.dhsdevelopments.dhsbuzz.gwt.users.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("UsersService")
public interface UsersService extends RemoteService
{
    ProfileList loadProfiles();

    /**
     * Utility/Convenience class.
     * Use UsersService.App.getInstance() to access static instance of UsersServiceAsync
     */
    public static class App
    {
        private static UsersServiceAsync ourInstance = GWT.create( UsersService.class );

        public static synchronized UsersServiceAsync getInstance() {
            return ourInstance;
        }
    }
}
