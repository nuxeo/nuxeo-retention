package org.nuxeo.retention.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.operations.services.FileManagerImport;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(EmbeddedAutomationServerFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types")
@Deploy("org.nuxeo.ecm.core.management")
@Deploy("org.nuxeo.ecm.platform.filemanager")
public class TestFrameIOAsset {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected BulkService bulkService;

    @Inject
    public TransactionalFeature txFeature;

    /**
     * Should Download a given asset, and create a Nuxeo document related to this blob. For testing purpose the asset is
     * available at {@value FrameIOUtils#SAMPLE_ASSET}
     * <p/>
     * Some keys points on the design:
     * <ul>
     * <li>Use the event architecture to fire a Nuxeo event, containing the needed infos
     * {@link FrameIOUtils#handleAsset(org.nuxeo.ecm.core.api.CoreSession, org.nuxeo.ecm.core.api.DocumentModel)}</li>
     * <li>The event listener handler {@link DownloadFrameIOAssetListener#handleEvent(org.nuxeo.ecm.core.event.Event)}
     * will handle the fired event and submit a command computation</li>
     * <li>The computation {@link DownloadAssetAction.DownloadAssetComputation} will be in charge of downloading the
     * asset and crete a Nuxeo document using the {@link FileManagerImport}
     * {@link DownloadAssetAction.DownloadAssetComputation#compute(CoreSession, List, Map)}</li>
     * </ul>
     */
    @Test
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/frameio-actions-test.xml")
    @Deploy("org.nuxeo.retention.core.test:OSGI-INF/frameio-events-test.xml")
    public void shouldDownloadFrameIOAsset() {
        // The logic might be updated, here I am passing the folder destination to the computation through the event
        DocumentModel folder = session.createDocumentModel("/", "myCustomAction", "Folder");
        folder = session.createDocument(folder);
        session.save();

        FrameIOUtils.handleAsset(session, folder);
        waitForAsyncCompletion();

        folder.refresh();
        DocumentModelList documents = session.getChildren(folder.getRef());
        assertEquals(1, documents.size());
        DocumentModel document = documents.get(0);
        Serializable content = document.getPropertyValue("file:content");
        assertTrue(content instanceof BinaryBlob);
        BinaryBlob binaryBlob = (BinaryBlob) content;
        assertFalse(binaryBlob.getDigest().isEmpty());
        assertEquals("image/jpeg", binaryBlob.getMimeType());
        assertEquals("bmw", binaryBlob.getFilename());
    }

    protected void waitForAsyncCompletion() {
        txFeature.nextTransaction();
        eventService.waitForAsyncCompletion();
    }
}
