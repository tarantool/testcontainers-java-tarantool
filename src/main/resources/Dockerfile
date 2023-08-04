ARG TARANTOOL_VERSION=2.11.0
FROM tarantool/tarantool:${TARANTOOL_VERSION}-centos7 AS cartridge-base

# system preparations because docker mount directory as a root
ARG TARANTOOL_SERVER_USER="root"
ARG TARANTOOL_SERVER_GROUP="root"
USER $TARANTOOL_SERVER_USER:$TARANTOOL_SERVER_GROUP
RUN groupadd $TARANTOOL_SERVER_GROUP && useradd -m -s /bin/bash $TARANTOOL_SERVER_USER || true

# install dependencies
# a yum bug requires setting ulimit, see https://bugzilla.redhat.com/show_bug.cgi?id=1537564
RUN ulimit -n 1024 && \
    yum -y install cmake make gcc gcc-c++ git unzip cartridge-cli && \
    yum clean all
RUN cartridge version

# build and run
FROM cartridge-base AS cartridge-app
ARG CARTRIDGE_SRC_DIR="cartridge"
ARG TARANTOOL_WORKDIR="/app"
ARG TARANTOOL_RUNDIR="/tmp/run"
ARG TARANTOOL_DATADIR="/tmp/data"
ARG TARANTOOL_LOGDIR="/tmp/log"
ARG TARANTOOL_INSTANCES_FILE="./instances.yml"
ARG TARANTOOL_CLUSTER_COOKIE="testapp-cluster-cookie"
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR
ENV TARANTOOL_RUNDIR=$TARANTOOL_RUNDIR
ENV TARANTOOL_DATADIR=$TARANTOOL_DATADIR
ENV TARANTOOL_LOGDIR=$TARANTOOL_LOGDIR
ENV TARANTOOL_INSTANCES_FILE=$TARANTOOL_INSTANCES_FILE
ENV TARANTOOL_CLUSTER_COOKIE=$TARANTOOL_CLUSTER_COOKIE
COPY $CARTRIDGE_SRC_DIR $TARANTOOL_WORKDIR
WORKDIR $TARANTOOL_WORKDIR
CMD cartridge build && cartridge start --run-dir=$TARANTOOL_RUNDIR --data-dir=$TARANTOOL_DATADIR \
    --log-dir=$TARANTOOL_LOGDIR --cfg=$TARANTOOL_INSTANCES_FILE
