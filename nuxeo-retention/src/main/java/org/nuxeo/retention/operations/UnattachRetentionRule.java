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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.operations;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.service.RetentionManager;

/**
 * @since 2023.1
 */
@Operation(id = UnattachRetentionRule.ID, category = RetentionConstants.RETENTION_CATEGORY, label = "Unattach Retention Rule and stop retention", description = " Unattaches the retention rule on a flexible Record document. Stops the current retention on the record document. Returns back the unretained record document.")
public class UnattachRetentionRule {

    public static final String ID = "Document.UnattachRetentionRule";

    @Context
    protected CoreSession session;

    @Context
    protected RetentionManager retentionManager;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        return retentionManager.unattachRule(doc, session);
    }

}
