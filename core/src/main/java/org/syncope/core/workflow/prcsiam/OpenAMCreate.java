/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.workflow.prcsiam;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.syncope.core.workflow.OSWorkflowComponent;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import com.opensymphony.workflow.WorkflowException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.workflow.Constants;

/**
 * TODO: remove ASAP!
 */
public class OpenAMCreate extends OSWorkflowComponent
        implements FunctionProvider {

    @Override
    public void execute(Map transientVars, Map args, PropertySet ps)
            throws WorkflowException {

        SyncopeUser syncopeUser = (SyncopeUser) transientVars.get(
                Constants.SYNCOPE_USER);

        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                    AdminTokenAction.getInstance());

            Map<String, Set<String>> map = new HashMap<String, Set<String>>();
            map.put("cn", Collections.singleton(
                    String.valueOf(syncopeUser.getId())));
            map.put("sn", Collections.singleton(
                    String.valueOf(syncopeUser.getId())));
            map.put("uid", Collections.singleton(
                    String.valueOf(syncopeUser.getId())));
            map.put("mail",
                    Collections.singleton(Utils.getUserId(syncopeUser)));
            map.put("userpassword",
                    Collections.singleton(syncopeUser.getPassword()));
            map.put("inetuserstatus", Collections.singleton("Inactive"));

            AMIdentityRepository repo =
                    new AMIdentityRepository(adminToken, "/");

            repo.createIdentity(IdType.USER,
                    String.valueOf(syncopeUser.getId()), map);

            SSOTokenManager.getInstance().destroyToken(adminToken);
        } catch (Throwable t) {
            log.error("While trying to create the user on OpenAM", t);
        }
    }
}
