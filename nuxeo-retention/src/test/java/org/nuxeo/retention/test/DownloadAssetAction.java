package org.nuxeo.retention.test;

import static org.nuxeo.ecm.core.bulk.BulkServiceImpl.STATUS_STREAM;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;
import static org.nuxeo.lib.stream.computation.AbstractComputation.OUTPUT_1;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.services.FileManagerImport;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.bulk.action.computation.AbstractBulkComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since 11.5
 */
public class DownloadAssetAction implements StreamProcessorTopology {

    public static final String ACTION_NAME = "downloadAssetAction";

    public static final String ACTION_FULL_NAME = "frameio/" + ACTION_NAME;

    public static final String PARAM_DESC = "description";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(DownloadAssetComputation::new,
                               List.of(INPUT_1 + ":" + ACTION_FULL_NAME, OUTPUT_1 + ":" + STATUS_STREAM))
                       .build();
    }

    public static class DownloadAssetComputation extends AbstractBulkComputation {

        protected String description;

        public DownloadAssetComputation() {
            super(ACTION_FULL_NAME);
        }

        @Override
        public void startBucket(String bucketKey) {
            BulkCommand command = getCurrentCommand();
            description = command.getParam(PARAM_DESC);
        }

        @Override
        protected void compute(CoreSession session, List<String> ids, Map<String, Serializable> properties) {
            // Computation should be peer an asset download.
            if (ids.size() > 1) {
                throw new IllegalArgumentException("Many folders destinations are given for the asset creation !");
            }

            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            String url = (String) properties.get("url");
            String fileName = FilenameUtils.getBaseName(url);
            String extension = FilenameUtils.getExtension(url);
            HttpGet request = new HttpGet(url);
            Path destination = null;
            try (CloseableHttpClient httpClient = httpClientBuilder.build();
                    CloseableHttpResponse response = httpClient.execute(request);
                    InputStream is = response.getEntity().getContent()) {
                // TODO Florent/Nelson?
                // Here for testing purpose I am using the temporary file as a destination. Should we consider a
                // specific directory destination by configuration
                destination = Files.createTempFile(fileName, "." + extension);
                // Should we consider the usage of ReadableByteChannel and FileChannel.transferFrom
                Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }

            // Use the File Manager Import operation to create the blob's document
            try (OperationContext ctx = new OperationContext(session)) {
                // TODO Florent/Nelson should we consider putting the temp blob in the transient store for GC purposes
                Blob blob = new FileBlob(destination.toFile(), true);
                blob.setFilename(fileName);
                ctx.put("currentDocument", ids.get(0));
                ctx.setInput(blob);
                DocumentModel document = (DocumentModel) Framework.getService(AutomationService.class)
                                                                  .run(ctx, FileManagerImport.ID);
            } catch (OperationException e) {
                throw new NuxeoException(e);
            }
        }
    }
}
