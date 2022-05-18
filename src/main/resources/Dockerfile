FROM centos:7 AS tarantool-base
ARG TARANTOOL_VERSION=2.8
ARG TARANTOOL_SERVER_USER="root"
ARG TARANTOOL_SERVER_GROUP="root"
ARG TARANTOOL_WORKDIR="/app"
ARG TARANTOOL_RUNDIR="/tmp/run"
ARG TARANTOOL_DATADIR="/tmp/data"
ARG TARANTOOL_INSTANCES_FILE="./instances.yml"
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR
ENV TARANTOOL_RUNDIR=$TARANTOOL_RUNDIR
ENV TARANTOOL_DATADIR=$TARANTOOL_DATADIR
ENV TARANTOOL_INSTANCES_FILE=$TARANTOOL_INSTANCES_FILE
RUN curl -L https://tarantool.io/installer.sh | VER=$TARANTOOL_VERSION /bin/bash -s -- --repo-only && \
    yum -y install cmake make gcc gcc-c++ git unzip tarantool tarantool-devel cartridge-cli && \
    yum clean all
RUN groupadd $TARANTOOL_SERVER_GROUP && useradd -m -s /bin/bash $TARANTOOL_SERVER_USER || true
USER $TARANTOOL_SERVER_USER:$TARANTOOL_SERVER_GROUP
RUN cartridge version

FROM tarantool-base AS cartridge-base
WORKDIR $TARANTOOL_WORKDIR
CMD cartridge build && cartridge start --run-dir=$TARANTOOL_RUNDIR --data-dir=$TARANTOOL_DATADIR --cfg=$TARANTOOL_INSTANCES_FILE
