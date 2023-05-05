ARG TARANTOOL_VERSION=2.10.5
FROM tarantool/tarantool:${TARANTOOL_VERSION}-centos7 AS cartridge-base

# system preparations because docker mount directory as a root
ARG TARANTOOL_SERVER_USER="root"
ARG TARANTOOL_SERVER_GROUP="root"
USER $TARANTOOL_SERVER_USER:$TARANTOOL_SERVER_GROUP
RUN groupadd $TARANTOOL_SERVER_GROUP && useradd -m -s /bin/bash $TARANTOOL_SERVER_USER || true

# install dependencies
RUN yum -y install cmake make gcc gcc-c++ git unzip cartridge-cli && \
    yum clean all
RUN cartridge version

# build
ARG CARTRIDGE_SRC_DIR="cartridge"
ARG TARANTOOL_WORKDIR="/app"
COPY $CARTRIDGE_SRC_DIR $TARANTOOL_WORKDIR
WORKDIR $TARANTOOL_WORKDIR
RUN cartridge build --verbose

ENV TARANTOOL_RUNDIR="/tmp/run"
ENV TARANTOOL_DATADIR="/tmp/data"
ENV TARANTOOL_INSTANCES_FILE="./instances.yml"

CMD cartridge build && cartridge start --run-dir=$TARANTOOL_RUNDIR --data-dir=$TARANTOOL_DATADIR --cfg=$TARANTOOL_INSTANCES_FILE
