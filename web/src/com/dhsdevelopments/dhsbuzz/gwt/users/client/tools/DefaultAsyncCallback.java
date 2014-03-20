package com.dhsdevelopments.dhsbuzz.gwt.users.client.tools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class DefaultAsyncCallback<T> implements AsyncCallback<T>
{
    @Override
    public void onFailure( Throwable caught ) {
        GWT.log( "Got exception from server", caught );
        throw new RuntimeException( "failure" );
    }
}
