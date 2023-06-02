FROM centos:7

ARG TARANTOOL_WORKDIR="/app"
ARG TARANTOOL_RUNDIR="/tmp/run"
ARG TARANTOOL_DATADIR="/tmp/data"
ARG SDK_TGT_DIR="/sdk"
ARG DOWNLOAD_SDK_URI=""
ARG SDK_VERSION="tarantool-enterprise-sdk-nogc64-2.10.6-0-r557.linux.x86_64"
ARG SDK_TGZ=$SDK_VERSION.tar.gz

ENV DOWNLOAD_SDK_URI=$DOWNLOAD_SDK_URI
ENV SDK_VERSION=$SDK_VERSION
ENV SDK_TGT_DIR=$SDK_TGT_DIR
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR
ENV TARANTOOL_RUNDIR=$TARANTOOL_RUNDIR
ENV TARANTOOL_DATADIR=$TARANTOOL_DATADIR
ENV TARANTOOL_TRANSPORT = "ssl"
ENV TARANTOOL_SSL_SERVER_CA_FILE = "/ssl/ca.crt"
ENV TARANTOOL_SSL_SERVER_CERT_FILE = "/ssl/server.crt"
ENV TARANTOOL_SSL_SERVER_KEY_FILE = "/ssl/server.key"
ENV TARANTOOL_SSL_CLIENT_CA_FILE = "/ssl/ca.crt"
ENV TARANTOOL_SSL_CLIENT_CERT_FILE = "/ssl/client.crt"
ENV TARANTOOL_SSL_CLIENT_KEY_FILE = "/ssl/client.key"

RUN curl https://curl.se/ca/cacert.pem -o /etc/pki/tls/certs/ca-bundle.crt && \
    yum -y install wget && \
    wget $DOWNLOAD_SDK_URI/$SDK_TGZ && \
    mkdir ./tmp_sdk && tar -xf $SDK_TGZ -C ./tmp_sdk && \
    mv ./tmp_sdk/tarantool-enterprise $SDK_TGT_DIR && rm $SDK_TGZ && \
    cp $SDK_TGT_DIR/tarantool /usr/bin/tarantool

RUN /usr/bin/tarantool/cartridge version

WORKDIR $TARANTOOL_WORKDIR
CMD /usr/bin/tarantool/cartridge build && /usr/bin/tarantool/cartridge start --run-dir=$TARANTOOL_RUNDIR --data-dir=$TARANTOOL_DATADIR --cfg=$TARANTOOL_INSTANCES_FILE
