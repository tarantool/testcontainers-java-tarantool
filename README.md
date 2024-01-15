# TestContainers Tarantool module

[![ubuntu-master](https://github.com/tarantool/testcontainers-java-tarantool/actions/workflows/ubuntu-master.yml/badge.svg)](https://github.com/tarantool/testcontainers-java-tarantool/actions/workflows/ubuntu-master.yml)

Testcontainers module for the [Tarantool](https://tarantool.io) database and application server and the [Tarantool Cartridge](https://tarantool.io/cartridge) framework.

See [testcontainers.org](https://www.testcontainers.org) for more information about TestContainers.

## Installation

Add the Maven dependency:

```xml
<dependency>
  <groupId>io.tarantool</groupId>
  <artifactId>testcontainers-java-tarantool</artifactId>
  <version>1.2.0</version>
</dependency>
```

## Usage Example

### Standalone Tarantool server

For default setup, you need to have a file `server.lua` in your `src/test/resources` folder with contents similar to
the following:

```lua
box.cfg {
    listen = 3301,
    memtx_memory = 128 * 1024 * 1024, -- 128 Mb
    -- log = 'file:/tmp/tarantool.log',
    log_level = 6,
}
-- API user will be able to login with this password
box.schema.user.create('api_user', { password = 'secret' })
-- API user will be able to create spaces, add or remove data, execute functions
box.schema.user.grant('api_user', 'read,write,execute', 'universe')
```

The most necessary part is exposing the port 3301 for external connections -- the container will not start without that
setting in the startup script.

Instantiate a generic TarantoolContainer and use it in your tests:

```java
public class SomeTest {

    @ClassRule
    static TarantoolContainer container = new TarantoolContainer();

    @BeforeAll
    public void setUp() {
        // Run some setup commands
        container.executeCommand("return 1, 2");
        // Or execute a script
        container.executeScript("org/testcontainers/containers/test.lua");
    }

    @Test
    public void testSomething() throws Exception {

        // Use properties provided by the container
        TarantoolCredentials credentials =
            new SimpleTarantoolCredentials(container.getUsername(), container.getPassword());
        TarantoolServerAddress serverAddress =
            new TarantoolServerAddress(container.getHost(), container.getPort());

        // Create TarantoolClient instance and use it in tests
        try (ClusterTarantoolTupleClient client = new ClusterTarantoolTupleClient(credentials, serverAddress)) {
            Optional<TarantoolSpaceMetadata> spaceMetadata = client.metadata().getSpaceByName("test");
            ...

            // Execute some commands in Tarantool instance for verification
            List<Object> result = container.executeCommand("return 1, 2");
            ...
        }
    ...
```

### Tarantool Cartridge cluster

For testing against Tarantool Cartridge you need to place a directory with the application code into the classpath
(for example, into src/test/resources directory). Suppose we have the following directory structure in there:

```tree
src/test/resources/
├── cartridge
│   ├── Dockerfile.build.cartridge
│   ├── Dockerfile.cartridge
│   ├── app
│   │   └── roles
│   │       ├── api_router.lua
│   │       ├── api_storage.lua
│   │       └── custom.lua
│   ├── cartridge.post-build
│   ├── cartridge.pre-build
│   ├── deps.sh
│   ├── init.lua
│   ├── instances.yml
│   ├── stateboard.init.lua
│   ├── test
│   │   ├── helper
│   │   │   ├── integration.lua
│   │   │   └── unit.lua
│   │   ├── helper.lua
│   │   ├── integration
│   │   │   └── api_test.lua
│   │   └── unit
│   │       └── sample_test.lua
│   ├── testapp-scm-1.rockspec
│   ├── tmp
│   └── topology.lua
```

The file `instances.yml` contains the Cartridge nodes configuration, which looks like this:

```yaml
testapp.router:
  advertise_uri: localhost:3301
  http_port: 8081

testapp.s1-master:
  advertise_uri: localhost:3302
  http_port: 8082

testapp.s1-replica:
  advertise_uri: localhost:3303
  http_port: 8083

testapp.s2-master:
  advertise_uri: localhost:3304
  http_port: 8084

testapp.s2-replica:
  advertise_uri: localhost:3305
  http_port: 8085
```

and the file `topology.lua` contains a custom script which sets up the cluster topology using the Cartridge API:

```lua
cartridge = require('cartridge')
replicasets = {{
    alias = 'app-router',
    roles = {'vshard-router', 'app.roles.custom', 'app.roles.api_router'},
    join_servers = {{uri = 'localhost:3301'}}
}, {
    alias = 's1-storage',
    roles = {'vshard-storage', 'app.roles.api_storage'},
    join_servers = {{uri = 'localhost:3302'}, {uri = 'localhost:3303'}}
}, {
    alias = 's2-storage',
    roles = {'vshard-storage', 'app.roles.api_storage'},
    join_servers = {{uri = 'localhost:3304'}, {uri = 'localhost:3305'}}
}}
return cartridge.admin_edit_topology({replicasets = replicasets})
```

Now we can set up a Cartridge container for tests:

```java
@Testcontainers
public class SomeOtherTest {

    @Container
    private static final TarantoolCartridgeContainer container =
        // Pass the classpath-relative paths of the instances configuration and topology script files
        new TarantoolCartridgeContainer("cartridge/instances.yml", "cartridge/topology.lua")
            // Tarantool URI, optional. Default is "localhost"
            .withRouterHost("localhost")
            // Binary port, optional. Default is 3301
            .withRouterPort(3301)
            // Cartridge HTTP API port, optional, 8081 is default
            .withAPIPort(8801)
            // Specify the actual username, default is "admin"
            .withRouterUsername("admin")
            // Specify the actual password, see the "cluster_cookie" parameter
            // in the cartridge.cfg({...}) call in your application.
            // Usually it can be found in the init.lua module
            .withRouterPassword("secret-cluster-cookie")
            // allows to reuse the container build once for faster testing
            .withReuse(true); 

    // Use the created container in tests
    public void testFoo() {
        // Execute Lua commands in the router instance
        List<Object> result = container.executeCommand("return profile_get(1)");

        // Instantiate a client connected to the router node
        TarantoolCredentials credentials = new SimpleTarantoolCredentials(getRouterUsername(), getRouterPassword());
        TarantoolServerAddress address = new TarantoolServerAddress(getRouterHost(), getRouterPort());
        TarantoolClientConfig config = TarantoolClientConfig.builder().withCredentials(credentials).build();
        try (ClusterTarantoolTupleClient client = new ClusterTarantoolTupleClient(config, address)) {
            // Do something with the client...
        }
    }
```

##### Environment variables of cartridge container and build arguments:
###### Build arguments:

This section describes the Docker image build arguments and environment variables inside the container. It is worth 
nothing that almost all build arguments listed here are passed into environment variables of the same name. At the 
moment, the following arguments are available to build the image:

- `CARTRIDGE_SRC_DIR` - directory on the host machine that contains all the .lua scripts needed to initialize and run 
cartridge. Defaults is `cartridge`. **Only as a build argument.**
- `TARANTOOL_WORKDIR` - a directory where all data will be stored: snapshots, wal logs and cartridge config files. 
Defaults is `/app`. Converts to an environment variable. It is not recommended to override via the `withEnv(...)` method.
- `TARANTOOL_RUNDIR` -  a directory where PID and socket files are stored. Defaults is `/tmp/run`. Converts to an 
environment variable. It is not recommended to override via the `withEnv(...)` method.
- `TARANTOOL_DATADIR` - a directory containing the instances working directories. Defaults is `/tmp/data`. Converts to 
an environment variable. It is not recommended to override via the `withEnv(...)` method.
- `TARANTOOL_LOGDIR` - the directory where log files are stored. Defaults is `/tmp/log`. Converts to an environment 
- variable. It is not recommended to override via the `withEnv(...)` method.
- `TARANTOOL_INSTANCES_FILE` - path to the configuration file. Defaults is `./instances.yml`. Converts to an environment
variable. It is not recommended to override via the `withEnv(...)` method.
- `START_DELAY` - the time after which cartridge will actually run after the container has started. Converts to an 
environment variable. It is not recommended to override via the `withEnv(...)` method.

You can set the Docker image build arguments using a map, which is passed as an input argument to the constructor when
creating a container in Java code. See example: https://github.com/tarantool/testcontainers-java-tarantool/blob/355d1e985bd10beca83bc7ca77f919a288709419/src/test/java/org/testcontainers/containers/TarantoolCartridgeBootstrapFromLuaWithFixedPortsTest.java#L111-L119

###### Environment variables:

To set an environment variable, use the `withEnv(...)` method of testcontainers API. Full list of variables the 
environments used in cartridge can be found here [link](https://www.tarantool.io/ru/doc/2.11/book/cartridge/cartridge_api/modules/cartridge/#cfg-opts-box-opts).

***Note:*** As shown in the previous section, some build arguments are converted to environment variables and used to
cartridge build at the image build stage.

An example of how to set the `TARANTOOL_CLUSTER_COOKIE` parameter: https://github.com/tarantool/testcontainers-java-tarantool/blob/355d1e985bd10beca83bc7ca77f919a288709419/src/test/java/org/testcontainers/containers/TarantoolCartridgeBootstrapFromLuaWithFixedPortsTest.java#L57-L82

##### Mapping ports

Often there is a need to connect to a container through a specific port. To achieve this goal it is necessary
to know the mapped port specified in the Java code. To get the mapped port, use the getMappedPort(...)` method of
testcontainers API. See examples: https://github.com/tarantool/testcontainers-java-tarantool/blob/ae580cb59d4621d7c11e5f80c394ec141d4c92b3/src/test/java/org/testcontainers/containers/TarantoolCartridgePortMappingTest.java#L22-L98


## License

See [LICENSE](LICENSE).

## Copyright

Copyright (c) 2020 Alexey Kuzin and other authors.

See [AUTHORS](AUTHORS) for contributors.
