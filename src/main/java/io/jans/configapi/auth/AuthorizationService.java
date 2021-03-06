/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.configapi.auth.util.AuthUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.container.ResourceInfo;
import java.io.Serializable;
import java.util.List;

public abstract class AuthorizationService implements Serializable {

    private static final long serialVersionUID = 4012335221233316230L;

    @Inject
    Logger log;
    
    @Inject
    AuthUtil authUtil;

    public abstract void processAuthorization(String token, ResourceInfo resourceInfo, String method,
            String path) throws Exception;

    protected Response getErrorResponse(Response.Status status, String detail) {
        return Response.status(status).entity(detail).build();
    }

    public List<String> getRequestedScopes(String path) {
        return authUtil.getRequestedScopes(path); 
    }
    
    public List<String> getRequestedScopes(ResourceInfo resourceInfo) {
        return authUtil.getRequestedScopes(resourceInfo);
    }

    public boolean validateScope(List<String> authScopes, List<String> resourceScopes) {
        return authUtil.validateScope(authScopes, resourceScopes);
    }   

}
