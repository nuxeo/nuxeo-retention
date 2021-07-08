package org.nuxeo.retention.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.5
 */
public class DownloadFrameIOAssetListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        // FIXME IIRC the query is needed! static scroller?
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        String query = String.format("SELECT * FROM Document WHERE %s = '%s'", NXQL.ECM_UUID,
                context.getSourceDocument().getId());
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", context.getProperty(FrameIOUtils.ASSET_URL_PROPERTY));
        // We might use the FrameIO's asset meta-data; name, mime type. Passed through the event context and pass them
        // to the computation
        BulkCommand bulkCommand = new BulkCommand.Builder(DownloadAssetAction.ACTION_NAME, query.toString(),
                SecurityConstants.SYSTEM_USERNAME).params(params).build();
        Framework.getService(BulkService.class).submit(bulkCommand);
    }
}
