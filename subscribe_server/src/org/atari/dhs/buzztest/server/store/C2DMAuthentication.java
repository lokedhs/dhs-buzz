package org.atari.dhs.buzztest.server.store;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "C2DMAuthentication")
@Table(name = "c2dm_authentication")
public class C2DMAuthentication implements Serializable
{
    @Id
    @Column(name = "account_name")
    private String accountName;

    @Basic
    @Column(name = "token")
    private String token;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "update_time")
    private Date lastC2DMUpdateTime;

    public C2DMAuthentication() {
    }

    public C2DMAuthentication( String accountName, String token ) {
        this.accountName = accountName;
        this.token = token;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName( String accountName ) {
        this.accountName = accountName;
    }

    public String getToken() {
        return token;
    }

    public void setToken( String token ) {
        this.token = token;
    }

    public Date getLastC2DMUpdateTime() {
        return lastC2DMUpdateTime;
    }

    public void setLastC2DMUpdateTime( Date lastC2DMUpdateTime ) {
        this.lastC2DMUpdateTime = lastC2DMUpdateTime;
    }
}
