ARG BASE_IMAGE

FROM ${BASE_IMAGE}

ARG BUILD_TAG
ARG SCM_REF
ARG VERSION

LABEL org.nuxeo.build-tag=$BUILD_TAG
LABEL org.nuxeo.scm-ref=$SCM_REF
LABEL org.nuxeo.version=$VERSION

COPY --chown=900:0 nuxeo-retention-package-*.zip /packages/
