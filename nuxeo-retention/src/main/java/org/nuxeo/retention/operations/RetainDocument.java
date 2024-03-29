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
package org.nuxeo.retention.operations;

import java.util.Calendar;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.retention.RetentionConstants;

/**
 * @since 11.1
 */
@Operation(id = RetainDocument.ID, category = RetentionConstants.RETENTION_CATEGORY, label = "Set as Record and Retain Until", description = "Turn the input document into a record and retain it until the until date. Returns back the retained document.")
public class RetainDocument {

    public static final String ID = "Document.Retain";

    @Context
    protected CoreSession session;

    @Param(name = "until", required = false, description = "If empty, the input document will be retained indeterminately")
    protected Calendar until;

    @Param(name = "flexible", required = false, description = "If true and if the document is not already a record, it will be turned into a flexible record, enforced otherwise")
    protected boolean flexible;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentRef docRef) {
        return run(session.getDocument(docRef));
    }

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        if (!doc.isRecord()) {
            if (flexible) {
                session.makeFlexibleRecord(doc.getRef());
            } else {
                session.makeRecord(doc.getRef());
            }
        }
        session.setRetainUntil(doc.getRef(), until != null ? until : CoreSession.RETAIN_UNTIL_INDETERMINATE, null);
        return session.getDocument(doc.getRef());
    }

}
