ARG TARANTOOL_VERSION=2.11.2-ubuntu20.04
FROM tarantool/tarantool:${TARANTOOL_VERSION} AS cartridge-base

# system preparations because docker mount directory as a root
#ARG TARANTOOL_SERVER_USER="root"
#ARG TARANTOOL_SERVER_GROUP="root"
#USER $TARANTOOL_SERVER_USER:$TARANTOOL_SERVER_GROUP
#RUN groupadd $TARANTOOL_SERVER_GROUP && useradd -m -s /bin/bash $TARANTOOL_SERVER_USER || true

# install dependencies
# a yum bug requires setting ulimit, see https://bugzilla.redhat.com/show_bug.cgi?id=1537564
RUN echo "test"
RUN touch /etc/apt/sources.list
RUN echo "deb http://mirror.yandex.ru/ubuntu focal main restricted\ndeb http://mirror.yandex.ru/ubuntu focal-updates main restricted\ndeb http://mirror.yandex.ru/ubuntu focal universe\ndeb http://mirror.yandex.ru/ubuntu focal-updates universe\ndeb http://mirror.yandex.ru/ubuntu focal multiverse\ndeb http://mirror.yandex.ru/ubuntu focal-updates multiverse\ndeb http://mirror.yandex.ru/ubuntu focal-backports main restricted universe multiverse\ndeb http://mirror.yandex.ru/ubuntu focal-security main restricted\ndeb http://mirror.yandex.ru/ubuntu focal-security universe\ndeb http://mirror.yandex.ru/ubuntu focal-security multiverse" > /etc/apt/sources.list
RUN apt-get -y update
RUN apt-get -y install build-essential
RUN apt-get -y install cmake
RUN apt-get -y install make
RUN apt-get -y install gcc
RUN apt-get -y install git
RUN apt-get -y install unzip
RUN apt-get -y install cartridge-cli
RUN cartridge version

# build and run
FROM cartridge-base AS cartridge-app
ARG CARTRIDGE_SRC_DIR="cartridge"
ARG TARANTOOL_WORKDIR="/app"
ARG TARANTOOL_RUNDIR="/tmp/run"
ARG TARANTOOL_DATADIR="/tmp/data"
ARG TARANTOOL_LOGDIR="/tmp/log"
ARG TARANTOOL_INSTANCES_FILE="./instances.yml"
ARG TARANTOOL_CLUSTER_COOKIE
ARG START_DELAY="5s"
ENV START_DELAY=$START_DELAY
ENV TARANTOOL_WORKDIR=$TARANTOOL_WORKDIR
ENV TARANTOOL_RUNDIR=$TARANTOOL_RUNDIR
ENV TARANTOOL_DATADIR=$TARANTOOL_DATADIR
ENV TARANTOOL_LOGDIR=$TARANTOOL_LOGDIR
ENV TARANTOOL_INSTANCES_FILE=$TARANTOOL_INSTANCES_FILE
ENV TARANTOOL_CLUSTER_COOKIE=$TARANTOOL_CLUSTER_COOKIE
ENV CMAKE_DUMMY_WEBUI="YES"
COPY $CARTRIDGE_SRC_DIR $TARANTOOL_WORKDIR
WORKDIR $TARANTOOL_WORKDIR

RUN rm -rf .rocks
RUN cartridge build --verbose

RUN echo 'if [ -z "$TARANTOOL_CLUSTER_COOKIE" ]; then unset TARANTOOL_CLUSTER_COOKIE ; fi ; \
    sleep $START_DELAY && cartridge start --run-dir=$TARANTOOL_RUNDIR --data-dir=$TARANTOOL_DATADIR \
        --log-dir=$TARANTOOL_LOGDIR --cfg=$TARANTOOL_INSTANCES_FILE' > run.sh && chmod +x run.sh
CMD ./run.sh
