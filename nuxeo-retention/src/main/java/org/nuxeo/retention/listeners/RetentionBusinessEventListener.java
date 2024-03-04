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
package org.nuxeo.retention.listeners;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.retention.RetentionConstants.ACTIVE_EVENT_BASED_RETENTION_RULES_QUERY;
import static org.nuxeo.retention.RetentionConstants.RECORD_RULE_IDS_PROP;
import static org.nuxeo.retention.RetentionConstants.RULE_RECORD_DOCUMENT_QUERY;
import static org.nuxeo.retention.RetentionConstants.STARTING_POINT_EVENT_PROP;
import static org.nuxeo.retention.actions.EvalInputEventBasedRuleAction.ACTION_EVENT_ID_PARAM;
import static org.nuxeo.retention.actions.EvalInputEventBasedRuleAction.ACTION_EVENT_INPUT_PARAM;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.retention.actions.EvalInputEventBasedRuleAction;
import org.nuxeo.retention.event.RetentionEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener processing events with a {@link org.nuxeo.retention.event.RetentionEventContext}). The listener schedules a
 * {@link org.nuxeo.retention.actions.EvalInputEventBasedRuleAction} on a query retrieving all the records attached to
 * all the retention rules targeting the listened event.
 *
 * @since 11.1
 */
public class RetentionBusinessEventListener implements EventListener {

    private static final Logger log = LogManager.getLogger(RetentionBusinessEventListener.class);

    @Override
    public void handleEvent(Event event) {
        EventContext evtCtx = event.getContext();
        if (evtCtx instanceof RetentionEventContext) {
            String eventName = event.getName();
            log.trace("Proceeding event {}", eventName);
            String eventInput = ((RetentionEventContext) evtCtx).getInput();
            BulkService bulkService = Framework.getService(BulkService.class);
            RepositoryService repositoryService = Framework.getService(RepositoryService.class);
            for (String repositoryName : repositoryService.getRepositoryNames()) {
                StringBuilder query = new StringBuilder(RULE_RECORD_DOCUMENT_QUERY);
                var rulesIds = getEventBasedRuleIdsForEvent(eventName, repositoryName);
                query.append(" AND ") //
                     .append(RECORD_RULE_IDS_PROP) //
                     .append(String.format(" IN ('%s')", rulesIds.stream().collect(Collectors.joining("', '"))));
                BulkCommand command = new BulkCommand.Builder(EvalInputEventBasedRuleAction.ACTION_NAME,
                        query.toString(), SYSTEM_USERNAME).param(ACTION_EVENT_ID_PARAM, eventName)
                                                          .param(ACTION_EVENT_INPUT_PARAM, eventInput)
                                                          .repository(repositoryName)
                                                          .build();
                bulkService.submit(command);
            }
        }
    }

    protected List<String> getEventBasedRuleIdsForEvent(String eventName, String repository) {
        StringBuilder query = new StringBuilder(ACTIVE_EVENT_BASED_RETENTION_RULES_QUERY);
        query.append(" AND ") //
             .append(STARTING_POINT_EVENT_PROP)
             .append(" = ")
             .append(NXQL.escapeString(eventName));
        CoreSession session = CoreInstance.getCoreSession(repository);
        PartialList<Map<String, Serializable>> results = session.queryProjection(query.toString(), 0, 0);
        return results.stream().map(m -> (String) m.get(ECM_UUID)).collect(Collectors.toList());
    }

}
