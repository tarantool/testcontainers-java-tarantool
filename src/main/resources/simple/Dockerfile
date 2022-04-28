FROM centos:7

ARG TARANTOOL_WORKDIR="/app"
ARG TARANTOOL_RUNDIR="/tmp/run"
ARG TARANTOOL_DATADIR="/tmp/data"
ARG SDK_TGT_DIR="/sdk"
ARG CREDS=""
ARG SDK_VERSION="tarantool-enterprise-bundle-2.8.3-21-g7d35cd2be-r470"
ARG SDK_TGZ=$SDK_VERSION.tar.gz

ENV CREDS=$CREDS
ENV SDK_VERSION=$SDK_VERSION
ENV SDK_TGT_DIR=$SDK_TGT_DIR
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR
ENV TARANTOOL_RUNDIR=$TARANTOOL_RUNDIR
ENV TARANTOOL_DATADIR=$TARANTOOL_DATADIR

RUN curl https://curl.se/ca/cacert.pem -o /etc/pki/tls/certs/ca-bundle.crt && \
    yum -y install wget && \
    wget https://${CREDS}@download.tarantool.io/enterprise/${SDK_TGZ} && \
    mkdir ./tmp_sdk && tar -xf $SDK_TGZ -C ./tmp_sdk && \
    mv ./tmp_sdk/tarantool-enterprise $SDK_TGT_DIR && rm $SDK_TGZ && \
    cp $SDK_TGT_DIR/tarantool /usr/bin/tarantool

WORKDIR $TARANTOOL_WORKDIR