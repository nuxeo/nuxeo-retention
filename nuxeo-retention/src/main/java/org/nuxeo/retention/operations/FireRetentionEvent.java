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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.service.RetentionManager;

/**
 * @since 11.1
 */
@Operation(id = FireRetentionEvent.ID, category = RetentionConstants.RETENTION_CATEGORY, label = "Fire Retention Event", description = "Fire a retention business related event. The record needs to be attached to a event based retention rule")
public class FireRetentionEvent {

    public static final String ID = "Retention.FireEvent";

    @Context
    protected OperationContext ctx;

    @Context
    protected RetentionManager retentionManager;

    @Param(name = "name", required = true)
    protected String name;

    @Param(name = "audit", required = false)
    protected boolean audit = true;

    @OperationMethod
    public void run() {
        retentionManager.fireRetentionEvent(name, ctx.getInput() instanceof String ? (String) ctx.getInput() : null,
                audit, ctx.getCoreSession());
    }

}
