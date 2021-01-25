FROM centos:7 AS tarantool-base
ENV TARANTOOL_VERSION=2.4
ENV TARANTOOL_SERVER_USER="tarantool"
ENV TARANTOOL_SERVER_UID=1000
ENV TARANTOOL_SERVER_GROUP="tarantool"
ENV TARANTOOL_SERVER_GID=1000
RUN curl -L https://tarantool.io/installer.sh | VER=$TARANTOOL_VERSION /bin/bash -s -- --repo-only && \
    yum -y install cmake make gcc git unzip tarantool tarantool-devel cartridge-cli && \
    yum clean all
RUN groupadd -g $TARANTOOL_SERVER_GID $TARANTOOL_SERVER_GROUP && \
    useradd -u $TARANTOOL_SERVER_UID -g $TARANTOOL_SERVER_GID -m -s /bin/bash $TARANTOOL_SERVER_USER \
    || true
USER $TARANTOOL_SERVER_USER:$TARANTOOL_SERVER_GROUP
RUN env && cartridge version

FROM tarantool-base AS cartridge-base
ENV TARANTOOL_WORKDIR="/app"
WORKDIR $TARANTOOL_WORKDIR
CMD env && cartridge build && cartridge start
