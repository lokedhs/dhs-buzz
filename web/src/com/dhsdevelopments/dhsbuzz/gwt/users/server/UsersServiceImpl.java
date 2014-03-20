package com.dhsdevelopments.dhsbuzz.gwt.users.server;

import com.dhsdevelopments.dhsbuzz.gwt.users.client.ProfileList;
import com.dhsdevelopments.dhsbuzz.gwt.users.client.UsersService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class UsersServiceImpl extends RemoteServiceServlet implements UsersService
{
    @Override
    public ProfileList loadProfiles() {
        return ProfileList.makeDemoList();
    }
}
