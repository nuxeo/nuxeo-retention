/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Calendar;

import jakarta.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.operations.AttachRetentionRule;
import org.nuxeo.retention.operations.RetainDocument;
import org.nuxeo.retention.operations.UnattachRetentionRule;

/**
 * @since 2023.1
 */
public class TestRetentionOperation extends RetentionTestCase {

    @Inject
    AutomationService service;

    @Test
    public void testAttachEnforcedRuleAndUnattach() throws OperationException {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
        RetentionRule rule = createManualImmediateRuleMillis(Duration.ofDays(1).toMillis());
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            OperationChain chain = new OperationChain("testChain");
            chain.add(AttachRetentionRule.ID).set("rule", rule.getDocument());
            file = (DocumentModel) service.run(ctx, chain);
            assertTrue(file.isRecord());
            assertTrue(file.isEnforcedRecord());
            assertTrue(file.isUnderRetentionOrLegalHold());
        }
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            OperationChain chain = new OperationChain("testChain");
            chain.add(UnattachRetentionRule.ID);
            assertThrows(PropertyException.class, () -> service.run(ctx, chain));
            file = session.getDocument(file.getRef());
            assertTrue(file.isRecord());
            assertTrue(file.isEnforcedRecord());
            assertTrue(file.isUnderRetentionOrLegalHold());
            assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        }
    }

    @Test
    public void testAttachFlexibleRuleAndUnattach() throws OperationException {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
        RetentionRule rule = createManualImmediateFlexibleRuleMillis(Duration.ofDays(1).toMillis());
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            OperationChain chain = new OperationChain("testChain");
            chain.add(AttachRetentionRule.ID).set("rule", rule.getDocument());
            file = (DocumentModel) service.run(ctx, chain);
            assertTrue(file.isRecord());
            assertTrue(file.isFlexibleRecord());
            assertTrue(file.isUnderRetentionOrLegalHold());
        }
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            OperationChain chain = new OperationChain("testChain");
            chain.add(UnattachRetentionRule.ID);
            file = (DocumentModel) service.run(ctx, chain);
            assertTrue(file.isRecord());
            assertTrue(file.isFlexibleRecord());
            assertFalse(file.isUnderRetentionOrLegalHold());
            assertFalse(file.hasFacet(RetentionConstants.RECORD_FACET));
        }
    }

    @Test
    public void testRetainEnforced() throws OperationException {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            OperationChain chain = new OperationChain("testChain");
            chain.add(RetainDocument.ID);
            file = (DocumentModel) service.run(ctx, chain);
            assertTrue(file.isRecord());
            assertTrue(file.isEnforcedRecord());
            assertTrue(file.isUnderRetentionOrLegalHold());
        }
    }

    @Test
    public void testRetainFlexible() throws OperationException {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
        Calendar retainUnitl = Calendar.getInstance();
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            OperationChain chain = new OperationChain("testChain");
            retainUnitl.add(Calendar.HOUR, 1);
            chain.add(RetainDocument.ID).set("flexible", true).set("until", retainUnitl);
            file = (DocumentModel) service.run(ctx, chain);
            assertTrue(file.isRecord());
            assertTrue(file.isFlexibleRecord());
            assertTrue(file.isUnderRetentionOrLegalHold());
        }
        try (OperationContext ctx = new OperationContext(session)) {
            ctx.setInput(file);
            OperationChain chain = new OperationChain("testChain");
            retainUnitl.add(Calendar.HOUR, 1);
            chain.add(RetainDocument.ID).set("until", retainUnitl);
            file = (DocumentModel) service.run(ctx, chain);
            assertTrue(file.isRecord());
            assertTrue(file.isFlexibleRecord());
            assertTrue(file.isUnderRetentionOrLegalHold());
        }
    }

}
