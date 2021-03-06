package io.jans.configapi.auth.service;

import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.common.ScopeType;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.auth.util.AuthUtil;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.service.ConfigurationService;
import io.jans.util.StringHelper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Named("patService")
public class PatService {

    @Inject
    Logger log;

    @Inject
    ConfigurationService configurationService;

    @Inject
    ConfigurationFactory configurationFactory;

    @Inject
    private AuthUtil authUtil;

    @Inject
    UmaMetadata umaMetadata;

    private Token umaPat;
    private long umaPatAccessTokenExpiration = 0l; // When the "accessToken" will expire;
    private final ReentrantLock lock = new ReentrantLock();

    public Token getPatToken() throws Exception {
        if (isValidPatToken(this.umaPat, this.umaPatAccessTokenExpiration)) {
            return this.umaPat;
        }

        lock.lock();
        try {
            if (isValidPatToken(this.umaPat, this.umaPatAccessTokenExpiration)) {
                return this.umaPat;
            }

            retrievePatToken();
        } finally {
            lock.unlock();
        }

        return this.umaPat;
    }

    protected boolean isEnabledUmaAuthentication() {
        return (umaMetadata != null) && isExistPatToken();
    }

    public boolean isExistPatToken() {
        try {
            return getPatToken() != null;
        } catch (Exception ex) {
            log.error("Failed to check UMA PAT token status", ex);
        }

        return false;
    }

    public String getIssuer() {
        if (umaMetadata == null) {
            return "";
        }
        return umaMetadata.getIssuer();
    }

    private void retrievePatToken() throws Exception {
        this.umaPat = null;
        if (umaMetadata == null) {
            return;
        }

        try {
            String clientId = authUtil.getClientId();
            this.umaPat = authUtil.requestPat(this.umaMetadata.getTokenEndpoint(), clientId, ScopeType.UMA, null);

            if (this.umaPat == null) {
                this.umaPatAccessTokenExpiration = 0l;
            } else {
                this.umaPatAccessTokenExpiration = computeAccessTokenExpirationTime(this.umaPat.getExpiresIn());
            }
        } catch (Exception ex) {
            throw new Exception("Failed to obtain valid UMA PAT token", ex);
        }

        if ((this.umaPat == null) || (this.umaPat.getAccessToken() == null)) {
            throw new Exception("Failed to obtain valid UMA PAT token");
        }
    }

    protected long computeAccessTokenExpirationTime(Integer expiresIn) {
        Calendar calendar = Calendar.getInstance();
        if (expiresIn != null) {
            calendar.add(Calendar.SECOND, expiresIn);
            calendar.add(Calendar.SECOND, -10); // Subtract 10 seconds to avoid expirations during executing request
        }

        return calendar.getTimeInMillis();
    }

    private boolean isValidPatToken(Token validatePatToken, long validatePatTokenExpiration) {
        final long now = System.currentTimeMillis();

        // Get new access token only if is the previous one is missing or expired
        return !((validatePatToken == null) || (validatePatToken.getAccessToken() == null)
                || (validatePatTokenExpiration <= now));
    }

}
