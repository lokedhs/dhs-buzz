package org.atari.dhs.buzztest.server.store;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity(name = "UnsubscriptionRequest")
public class UnsubscriptionRequest implements Serializable
{
    @Id
    @Column(name = "commentsUrl")
    private String commentsUrl;

    @Basic
    @Column(name = "verificationId")
    private String verificationId;

    @Basic
    @Column(name = "verified")
    private boolean verified;

    public UnsubscriptionRequest() {
    }

    public UnsubscriptionRequest( String commentsUrl, String verificationId ) {
        this.commentsUrl = commentsUrl;
        this.verificationId = verificationId;
    }

    public String getCommentsUrl() {
        return commentsUrl;
    }

    public void setCommentsUrl( String commentsUrl ) {
        this.commentsUrl = commentsUrl;
    }

    public String getVerificationId() {
        return verificationId;
    }

    public void setVerificationId( String verificationId ) {
        this.verificationId = verificationId;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified( boolean verified ) {
        this.verified = verified;
    }
}
