/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.retention.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.operations.HoldDocument;
import org.nuxeo.retention.operations.RetainDocument;
import org.nuxeo.retention.operations.UnholdDocument;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 2023
 */
public class TestRetentionOperation extends RetentionTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void testRetainUntilOperation() throws OperationException, IOException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(file);
            Map<String, Serializable> params = new HashMap<>();
            Calendar aSecond = Calendar.getInstance();
            aSecond.add(Calendar.SECOND, 1);
            params.put("until", aSecond);
            file = (DocumentModel) automationService.run(context, RetainDocument.ID, params);
            assertTrue(file.isRecord());
            assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertRetainedProperties(new String[0], file.getAdapter(Record.class));
        }
    }

    @Test
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/retention-retainable-files-contrib-test.xml")
    public void testRetainUntilWithAttachementsOperation() throws OperationException, IOException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(file);
            Map<String, Serializable> params = new HashMap<>();
            Calendar aSecond = Calendar.getInstance();
            aSecond.add(Calendar.SECOND, 1);
            params.put("until", aSecond);
            file = (DocumentModel) automationService.run(context, RetainDocument.ID, params);
            assertTrue(file.isRecord());
            assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertRetainedProperties(new String[] { "files/*/file" }, file.getAdapter(Record.class));
        }
    }

    @Test
    public void testLegalHoldOperation() throws OperationException, IOException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(file);
            file = (DocumentModel) automationService.run(context, HoldDocument.ID);
            assertTrue(file.isRecord());
            assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertRetainedProperties(new String[0], file.getAdapter(Record.class));
        }
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(file);
            file = (DocumentModel) automationService.run(context, UnholdDocument.ID);
            assertTrue(file.isRecord());
            assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertRetainedProperties(new String[0], file.getAdapter(Record.class));
        }
    }

    @Test
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/retention-retainable-files-contrib-test.xml")
    public void testLegalHoldWithAttachementsOperation() throws OperationException, IOException {
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(file);
            file = (DocumentModel) automationService.run(context, HoldDocument.ID);
            assertTrue(file.isRecord());
            assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertRetainedProperties(new String[] { "files/*/file" }, file.getAdapter(Record.class));
        }
        try (OperationContext context = new OperationContext(session)) {
            context.setInput(file);
            file = (DocumentModel) automationService.run(context, UnholdDocument.ID);
            assertTrue(file.isRecord());
            assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
            assertRetainedProperties(new String[0], file.getAdapter(Record.class));
        }
    }
}
