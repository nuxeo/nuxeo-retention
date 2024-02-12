/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this doc except in compliance with the License.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.event.RetentionEventContext;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 11.1
 */
public class TestRetentionManager extends RetentionTestCase {

    @Inject
    protected EventProducer eventProducer;

    @Test
    public void testRuleOnlyFile() {
        DocumentModel workspace = session.createDocumentModel("/", "workspace", "Workspace");
        workspace = session.createDocument(workspace);
        workspace = session.saveDocument(workspace);
        try {
            service.attachRule(workspace, createManualImmediateRuleMillis(100), session);
            fail("Should not accept workspace document");
        } catch (NuxeoException e) {
            assertEquals("Rule does not accept this document type", e.getMessage());
        }
    }

    @Test
    public void test1DayManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, null, 0, 0, 1, 0, null, null),
                1000);
    }

    @Test
    public void test1MonthManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, null, 0, 1, 0, 0, null, null),
                1000);
    }

    @Test
    public void test1YearManualImmediateRuleRunningRetention() throws InterruptedException {
        assertStillUnderRetentionAfter(file, createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.IMMEDIATE, null, null, null, null, null, 1, 0, 0, 0, null, null),
                1000);
    }

    @Test
    public void testManualImmediateRuleWithDefaultOperationActions() throws InterruptedException {
        RetentionRule testRule = createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, 100, null,
                List.of("Document.Trash"));

        file = service.attachRule(file, testRule, session);
        assertTrue(session.isRecord(file.getRef()));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000);

        file = session.getDocument(file.getRef());

        // it has no retention anymore and trashed
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(file.isTrashed());
    }

    @Test
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/retention-vocabularies-test.xml")
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/retention-automation-contrib-test.xml")
    public void testManualImmediateRuleWithCustomOperationActions() throws InterruptedException {
        RetentionRule testRule = createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, 100, null,
                List.of("MyCustomChain"));

        file = service.attachRule(file, testRule, session);
        assertTrue(session.isRecord(file.getRef()));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000);
        file = session.getDocument(file.getRef());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));

        // Made by the first operation (Document.Update) of the myCustomChain
        assertEquals("My New Title", file.getTitle());
        assertEquals("My New Description", file.getProperty("dublincore", "description"));

        // Made by the second operation (Document.AddFacet) of the myCustomChain
        assertTrue(file.hasFacet("MyFacet"));
    }

    @Test
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/retention-vocabularies-test.xml")
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/retention-scripting-contrib-test.xml")
    public void testManualImmediateRuleWithCustomScriptingActions() throws InterruptedException {
        RetentionRule testRule = createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, 100, null,
                List.of("MyCustomScripting"));

        file = service.attachRule(file, testRule, session);
        assertTrue(session.isRecord(file.getRef()));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000);
        file = session.getDocument(file.getRef());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));

        assertEquals("Update Title From Scripting", file.getTitle());
    }

    @Test
    public void testManualImmediateRule() throws InterruptedException {
        RetentionRule testRule = createManualImmediateRuleMillis(100);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertFalse(file.isLocked());

        awaitRetentionExpiration(1000);

        file = session.getDocument(file.getRef());

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testManualEventBasedRuleOnDocumentMovedToFolder() throws InterruptedException {
        RetentionRule testRule = createManualEventBasedRuleMillisWithExpression(DocumentEventTypes.DOCUMENT_MOVED,
                "document.getPathAsString().startsWith('/testFolder')", 1000);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        Record record = file.getAdapter(Record.class);
        assertTrue(record.isRetentionIndeterminate());

        awaitRetentionExpiration(500);

        file = session.getDocument(file.getRef());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        record = file.getAdapter(Record.class);
        assertTrue(record.isRetentionIndeterminate());

        DocumentModel folder = session.createDocumentModel("/", "testFolder", "Folder");
        folder = session.createDocument(folder);
        folder = session.saveDocument(folder);

        file = session.move(file.getRef(), folder.getRef(), null);

        awaitRetentionExpiration(500);

        record = file.getAdapter(Record.class);
        assertFalse(record.isRetentionIndeterminate());
        assertFalse(record.isRetentionExpired());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(500);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionExpired());
    }

    @Test
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/retention-vocabularies-test.xml")
    public void testManualEventBasedRuleWithInputOnCustomEvent() throws InterruptedException {
        String retentionEventId = "myRetentionEvent";
        String myRetentionEventInput = "myEventInput";
        RetentionRule testRule = createManualEventBasedRuleMillisWithEventValue(retentionEventId, myRetentionEventInput,
                1000);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(500);

        file = session.getDocument(file.getRef());
        Record record = file.getAdapter(Record.class);
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionIndeterminate());

        RetentionEventContext evctx = new RetentionEventContext(session.getPrincipal());
        evctx.setInput(myRetentionEventInput);
        Event event = evctx.newEvent(retentionEventId);
        eventProducer.fireEvent(event);

        awaitRetentionExpiration(500);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(file.isRecord());
        assertFalse(record.isRetentionIndeterminate());

        awaitRetentionExpiration(500);

        // it has no retention anymore
        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionExpired());
    }

    @Test
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/retention-vocabularies-test.xml")
    public void testManualEventBasedRuleWithExpressionOnCustomEvent() throws InterruptedException {
        String retentionEventId = "myRetentionEvent";
        String triggeringEventValue = "foo";
        String myRetentionEventExpression = "document.getTitle().equals(eventInput)";
        RetentionRule testRule = createManualEventBasedRuleMillisWithExpression(retentionEventId,
                myRetentionEventExpression, 1000);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(500);

        file = session.getDocument(file.getRef());
        Record record = file.getAdapter(Record.class);
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionIndeterminate());

        // Trigger event with unexpected input
        service.fireRetentionEvent(retentionEventId, triggeringEventValue, false, session);
        awaitRetentionExpiration(500);
        // Check record is still under indeterminate retention
        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionIndeterminate());

        // Trigger event with expected input
        file.setPropertyValue("dc:title", triggeringEventValue);
        file = session.saveDocument(file);
        service.fireRetentionEvent(retentionEventId, triggeringEventValue, false, session);
        awaitRetentionExpiration(500);
        // Check record is no longer under indeterminate retention
        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(file.isRecord());
        assertFalse(record.isRetentionIndeterminate());

        awaitRetentionExpiration(500);
        // it has no retention anymore
        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionExpired());
    }

    @Test
    public void testManualMetadataBasedRule() throws InterruptedException {
        RetentionRule testRule = createManualMetadataBasedRuleMillis("dc:expired", 1000);
        Calendar halfSecond = Calendar.getInstance();
        halfSecond.add(Calendar.MILLISECOND, 500);
        file.setPropertyValue("dc:expired", halfSecond);
        file = session.saveDocument(file);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000);

        Record record = file.getAdapter(Record.class);
        assertFalse(record.isRetentionIndeterminate());
        assertFalse(record.isRetentionExpired());
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        awaitRetentionExpiration(1000);

        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        // it has no retention anymore
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertTrue(record.isRetentionExpired());
    }

    @Test
    public void testManualPastMetadataBasedRule() {
        RetentionRule testRule = createManualMetadataBasedRuleMillis("dc:expired", 500);
        Calendar minusOneSecond = Calendar.getInstance();
        minusOneSecond.add(Calendar.MILLISECOND, -1000);
        file.setPropertyValue("dc:expired", minusOneSecond);
        file = session.saveDocument(file);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testManualNullMetadataBasedRule() {
        RetentionRule testRule = createManualMetadataBasedRuleMillis("dc:expired", 500);
        Calendar minusOneSecond = Calendar.getInstance();
        minusOneSecond.add(Calendar.MILLISECOND, -1000);
        file = session.saveDocument(file);

        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testRetainUntilDateSaved() throws InterruptedException {
        RetentionRule testRule = createManualImmediateRuleMillis(100);
        file = service.attachRule(file, testRule, session);
        Calendar original = file.getRetainUntil();
        awaitRetentionExpiration(1000);
        Record record = session.getDocument(file.getRef()).getAdapter(Record.class);
        Calendar saved = record.getSavedRetainUntil();
        assertNotNull(saved);
        assertEquals(original.getTimeInMillis(), saved.getTimeInMillis());
    }

    @Test
    public void testAttachRuleUnattachAndReattachRule() throws InterruptedException {
        RetentionRule testRule = createManualImmediateFlexibleRuleMillis(Duration.ofDays(1).toMillis());
        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(file.isFlexibleRecord());
        assertFalse(file.isEnforcedRecord());
        assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));

        // Unattach the rule
        file = service.unattachRule(file, session);
        assertTrue(file.isRecord());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
        assertFalse(file.hasFacet(RetentionConstants.RECORD_FACET));

        // Reattach another rule and wait it expires
        RetentionRule otherRule = createManualImmediateFlexibleRuleMillis(100);
        file = service.attachRule(file, otherRule, session);
        assertTrue(file.isRecord());
        assertTrue(file.isFlexibleRecord());
        assertFalse(file.isEnforcedRecord());
        assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
        awaitRetentionExpiration(500);
        file = session.getDocument(file.getRef());
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testReattachFlexibleRuleOnExpiredFlexibleRecord() {
        RetentionRule testRule = createManualImmediateFlexibleRuleMillis(1);
        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(file.isFlexibleRecord());
        assertFalse(file.isEnforcedRecord());
        assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));

        testRule = createManualImmediateFlexibleRuleMillis(Duration.ofDays(1).toMillis());
        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(file.isFlexibleRecord());
        assertFalse(file.isEnforcedRecord());
        assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testReattachEnforcedRuleOnExpiredFlexibleRecord() {
        RetentionRule testRule = createManualImmediateFlexibleRuleMillis(1);
        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertTrue(file.isFlexibleRecord());
        assertFalse(file.isEnforcedRecord());
        assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));

        testRule = createManualImmediateRuleMillis(Duration.ofDays(1).toMillis());
        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertFalse(file.isFlexibleRecord());
        assertTrue(file.isEnforcedRecord());
        assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        assertTrue(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

    @Test
    public void testReattachFlexibleRuleOnExpiredEnforcedRecord() throws InterruptedException {
        RetentionRule testRule = createManualImmediateRuleMillis(1);
        file = service.attachRule(file, testRule, session);
        assertTrue(file.isRecord());
        assertFalse(file.isFlexibleRecord());
        assertTrue(file.isEnforcedRecord());
        assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));

        RetentionRule testRule1Day = createManualImmediateFlexibleRuleMillis(Duration.ofDays(1).toMillis());
        assertThrows(IllegalStateException.class, () -> service.attachRule(file, testRule1Day, session));
        assertTrue(file.isRecord());
        assertFalse(file.isFlexibleRecord());
        assertTrue(file.isEnforcedRecord());
        assertTrue(file.hasFacet(RetentionConstants.RECORD_FACET));
        assertFalse(session.isUnderRetentionOrLegalHold(file.getRef()));
    }

}
