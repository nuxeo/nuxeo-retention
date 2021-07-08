package org.nuxeo.retention.test;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.0
 */
public class FrameIOUtils {

    public final static String DOWNLOAD_ASSET_EVENT_NAME = "frameioAssetContentAvailable";

    public final static String ASSET_URL_PROPERTY = "frameioAssetUrlProperty";

    public final static String SAMPLE_ASSET = "https://nuxeo-napps-sample-data.s3-eu-west-1.amazonaws.com/bmw.jpeg";

    /**
     * Handles the FrameIO's asset download.
     * 
     * @param session the session
     * @param document the document model related to the custom action
     */
    public static void handleAsset(CoreSession session, DocumentModel document) {
        EventService eventService = Framework.getService(EventService.class);
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), document);
        // We might pass the FrameIO's asset meta-data: name, mime type...
        ctx.getProperties().put(ASSET_URL_PROPERTY, SAMPLE_ASSET);
        eventService.fireEvent(ctx.newEvent(DOWNLOAD_ASSET_EVENT_NAME));
    }

    private FrameIOUtils() {
        // Not allowed
    }
}
