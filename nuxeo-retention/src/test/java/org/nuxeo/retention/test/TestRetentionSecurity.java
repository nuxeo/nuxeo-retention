/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Guillaume RENARD
 */
package org.nuxeo.retention.test;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.retention.RetentionConstants.RECORD_MANAGER_GROUP_NAME;

import java.util.Collections;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.event.RetentionEventContext;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 11.1
 */
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-security.xml")
public class TestRetentionSecurity extends RetentionTestCase {

    @Test
    public void shouldNotBeAuthorizedToManageLegalHold() {
        try {
            CoreSession userSession = CoreInstance.getCoreSession(session.getRepositoryName(), "user");
            service.attachRule(file, createManualImmediateRuleMillis(100), userSession);
            fail("Sould not be abe to attach rule");
        } catch (NuxeoException e) {
            assertEquals("User is not authorized to attach retention rule", e.getMessage());
            assertEquals(SC_FORBIDDEN, e.getStatusCode());
            assertFalse(session.isRecord(file.getRef()));
            assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertFalse(session.getDocument(file.getRef()).hasFacet(RetentionConstants.RECORD_FACET));
        }
    }

    @Test
    public void shouldNotBeAuthorizedToAttachRule() {
        try {
            CoreSession userSession = CoreInstance.getCoreSession(session.getRepositoryName(), "user");
            service.attachRule(file, createManualImmediateRuleMillis(100), userSession);
            fail("Sould not be abe to attach rule");
        } catch (NuxeoException e) {
            assertEquals("User is not authorized to attach retention rule", e.getMessage());
            assertEquals(SC_FORBIDDEN, e.getStatusCode());
            assertFalse(session.isRecord(file.getRef()));
            assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertFalse(session.getDocument(file.getRef()).hasFacet(RetentionConstants.RECORD_FACET));
        }
    }

    @Test
    public void shouldBeAuthorizedToAttachAndUnattachRule() {
        ACP acp = new ACPImpl();
        ACE allowAttachRule = new ACE("user", RetentionConstants.MANAGE_RECORD_PERMISSION, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowAttachRule });
        acp.addACL(acl);
        file.setACP(acp, true);
        file = session.saveDocument(file);
        CoreSession userSession = CoreInstance.getCoreSession(session.getRepositoryName(), "user");
        file = service.attachRule(file, createManualImmediateFlexibleRuleMillis(5000), userSession);
        assertTrue(userSession.isUnderRetentionOrLegalHold(file.getRef()));
        file = service.unattachRule(file, userSession);
        assertFalse(userSession.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void shouldNotBeAuthorizedToUnattachRule() {
        ACP acp = new ACPImpl();
        ACE allowReadWrite = new ACE("user", SecurityConstants.READ_WRITE, true);
        ACE allowMakeRecord = new ACE("user", SecurityConstants.MAKE_RECORD, true);
        ACE allowSetRetention = new ACE("user", SecurityConstants.SET_RETENTION, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowReadWrite, allowMakeRecord, allowSetRetention });
        acp.addACL(acl);
        file.setACP(acp, true);
        file = session.saveDocument(file);
        CoreSession userSession = CoreInstance.getCoreSession(session.getRepositoryName(), "user");
        file = service.attachRule(file, createManualImmediateFlexibleRuleMillis(5000), userSession);
        assertTrue(userSession.isUnderRetentionOrLegalHold(file.getRef()));
        assertThrows("User does have UnseRetention granted", NuxeoException.class,
                () -> service.unattachRule(file, userSession));
        assertTrue(userSession.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void shouldBeAuthorizedToSetLegalHold() {
        ACP acp = new ACPImpl();
        ACE allowLegalHold = new ACE("user", RetentionConstants.MANAGE_LEGAL_HOLD_PERMISSION, true);
        ACL acl = new ACLImpl();
        acl.setACEs(new ACE[] { allowLegalHold });
        acp.addACL(acl);
        file.setACP(acp, true);
        file = session.saveDocument(file);
        CoreSession userSession = CoreInstance.getCoreSession(session.getRepositoryName(), "user");
        userSession.makeRecord(file.getRef());
        userSession.setLegalHold(file.getRef(), true, null);
    }

    @Test
    public void shouldNotBeAllowedToAttachRuleOnDocAlreadyUnderRetention() {
        RetentionRule rr = createManualImmediateRuleMillis(100);
        file = service.attachRule(file, rr, session);
        assertThrows("Document is already under retention or legal hold", NuxeoException.class,
                () -> service.attachRule(file, rr, session));
    }

    @Test
    public void shouldNotBeAllowedToFireRetentionEvent() {
        CoreSession userSession = CoreInstance.getCoreSession(session.getRepositoryName(), "user");
        assertThrows("User should not be able to fire retention event", NuxeoException.class,
                () -> service.fireRetentionEvent("foo", "bar", true, userSession));
    }

    @Test
    public void shouldBeAllowedToFireRetentionEvent() {
        CoreSession userSession = CoreInstance.getCoreSession(session.getRepositoryName(), "user");
        userSession.getPrincipal().setGroups(Collections.singletonList(RECORD_MANAGER_GROUP_NAME));
        try (CapturingEventListener listener = new CapturingEventListener("foo")) {
            service.fireRetentionEvent("foo", "bar", false, userSession);
            assertEquals(1, listener.getCapturedEvents().size());
            Event event = listener.getCapturedEvents().get(0);
            assertEquals("foo", event.getName());
            assertTrue(event.getContext() instanceof RetentionEventContext);
            assertEquals(((RetentionEventContext) event.getContext()).getInput(), "bar");
        }
    }

}
