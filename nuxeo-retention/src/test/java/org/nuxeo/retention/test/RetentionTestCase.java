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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.security.RetentionExpiredFinderListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.adapters.RetentionRule.StartingPointPolicy;
import org.nuxeo.retention.service.RetentionManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(RetentionFeature.class)
public abstract class RetentionTestCase {

    @Inject
    protected RetentionManager service;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected TransactionalFeature transactionFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected BulkService bulkService;

    protected DocumentModel file;

    @Before
    public void setup() {
        file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
    }

    protected void assertStillUnderRetentionAfter(DocumentModel doc, RetentionRule rule, int timeoutMillis)
            throws InterruptedException {
        doc = service.attachRule(doc, rule, session);
        assertTrue(doc.isRecord());

        awaitRetentionExpiration(timeoutMillis);

        doc = session.getDocument(doc.getRef());

        // it is still under retention and has a retention date
        assertTrue(session.isUnderRetentionOrLegalHold(doc.getRef()));
        assertNotNull(session.getRetainUntil(doc.getRef()));
    }

    protected void awaitRetentionExpiration(long millis) throws InterruptedException {
        // wait a bit more than retention period to pass retention expiration date
        transactionFeature.nextTransaction();
        Thread.sleep(millis);
        // trigger manually instead of waiting for scheduler
        new RetentionExpiredFinderListener().handleEvent(null);
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        transactionFeature.nextTransaction();
    }

    protected RetentionRule createRuleWithActions(RetentionRule.ApplicationPolicy policy,
            StartingPointPolicy startingPointPolicy, List<String> docTypes, String startingPointEventId,
            String startingPointExpression, String startingPointValue, String metadataXPath, long years, long months,
            long days, long durationMillis, List<String> beginActions, List<String> endActions) {
        return createRuleWithActions(policy, startingPointPolicy, docTypes, startingPointEventId,
                startingPointExpression, startingPointValue, metadataXPath, years, months, days, durationMillis,
                beginActions, endActions, false);
    }

    protected RetentionRule createRuleWithActions(RetentionRule.ApplicationPolicy policy,
            StartingPointPolicy startingPointPolicy, List<String> docTypes, String startingPointEventId,
            String startingPointExpression, String startingPointValue, String metadataXPath, long years, long months,
            long days, long durationMillis, List<String> beginActions, List<String> endActions, boolean flexible) {
        DocumentModel doc = session.createDocumentModel("/RetentionRules", "testRule" + +System.currentTimeMillis(),
                "RetentionRule");
        RetentionRule rule = doc.getAdapter(RetentionRule.class);
        rule.setDurationYears(years);
        rule.setDurationMonths(months);
        rule.setDurationDays(days);
        rule.setDurationMillis(durationMillis);
        rule.setApplicationPolicy(policy);
        rule.setStartingPointPolicy(startingPointPolicy);
        rule.setDocTypes(docTypes);
        rule.setStartingPointEvent(startingPointEventId);
        rule.setStartingPointExpression(startingPointExpression);
        rule.setStartingPointValue(startingPointValue);
        rule.setMetadataXpath(metadataXPath);
        rule.setBeginActions(beginActions);
        rule.setEndActions(endActions);
        if (flexible) {
            rule.makeFlexibleRecord();
        } else {
            rule.makeEnforcedRecord();
        }
        doc = session.createDocument(doc);
        return session.saveDocument(doc).getAdapter(RetentionRule.class);
    }

    protected RetentionRule createImmediateRuleMillis(RetentionRule.ApplicationPolicy policy, long durationMillis,
            List<String> beginActions, List<String> endActions) {
        return createImmediateRuleMillis(policy, durationMillis, beginActions, endActions, false);
    }

    protected RetentionRule createImmediateRuleMillis(RetentionRule.ApplicationPolicy policy, long durationMillis,
            List<String> beginActions, List<String> endActions, boolean flexible) {
        return createRuleWithActions(policy, RetentionRule.StartingPointPolicy.IMMEDIATE, List.of("File"), null, null,
                null, null, 0L, 0L, 0L, durationMillis, beginActions, endActions, flexible);
    }

    protected RetentionRule createManualImmediateRuleMillis(long durationMillis) {
        return createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, durationMillis, null, null, false);
    }

    protected RetentionRule createManualImmediateFlexibleRuleMillis(long durationMillis) {
        return createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, durationMillis, null, null, true);
    }

    protected RetentionRule createManualEventBasedRuleMillisWithExpression(String eventId,
            String startingPointExpression, long durationMillis) {
        return createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.EVENT_BASED, null, eventId, startingPointExpression, null, null, 0L,
                0L, 0L, durationMillis, null, null);
    }

    protected RetentionRule createManualEventBasedRuleMillisWithEventValue(String eventId, String startingPointValue,
            long durationMillis) {
        return createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.EVENT_BASED, null, eventId, null, startingPointValue, null, 0L, 0L,
                0L, durationMillis, null, null);
    }

    protected RetentionRule createManualEventBasedRuleMillisWithEventExpressionValue(String eventId, String expression,
            long durationMillis) {
        return createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.EVENT_BASED, null, eventId, expression, null, null, 0L, 0L, 0L,
                durationMillis, null, null);
    }

    protected RetentionRule createManualMetadataBasedRuleMillis(String metadataXPath, long durationMillis) {
        return createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.METADATA_BASED, null, null, null, null, metadataXPath, 0L, 0L, 0L,
                durationMillis, null, null);
    }

}
