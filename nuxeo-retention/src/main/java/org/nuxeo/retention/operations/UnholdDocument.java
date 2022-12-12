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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.service.RetentionManager;

/**
 * @since 11.1
 */
@Operation(id = UnholdDocument.ID, category = Constants.CAT_DOCUMENT, label = "Remove Legal Hold", description = "Remove a legal hold on the input document. Returns back the unhold document.")
public class UnholdDocument {

    public static final String ID = "Document.Unhold";

    @Context
    protected CoreSession session;

    @Context
    protected RetentionManager retentionManager;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        // We only add record facet for backward compat
        doc.addFacet(RetentionConstants.RECORD_FACET);
        Record record = doc.getAdapter(Record.class);
        retentionManager.setLegalHold(session, record, false, null);
        return session.getDocument(doc.getRef());
    }

}
