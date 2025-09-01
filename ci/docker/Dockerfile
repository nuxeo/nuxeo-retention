ARG NUXEO_VERSION=2023

FROM docker-private.packages.nuxeo.com/nuxeo/nuxeo:${NUXEO_VERSION}

ARG BUILD_TAG=unknown
ARG SCM_REF=unknown
ARG VERSION=unknown

LABEL org.nuxeo.retention.build-tag=$BUILD_TAG
LABEL org.nuxeo.retention.scm-ref=$SCM_REF
LABEL org.nuxeo.retention.version=$VERSION

RUN --mount=type=secret,id=CLID,env=CLID \
    --mount=type=secret,id=CONNECT_URL,env=CONNECT_URL \
    /install-packages.sh --clid ${CLID} --connect-url ${CONNECT_URL} \
      nuxeo-web-ui

RUN --mount=type=bind,source=./target,target=/tmp/local-packages  \
    /install-packages.sh --offline /tmp/local-packages/*.zip
