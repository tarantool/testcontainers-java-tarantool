# TestContainers Tarantool module

[![tarantool-testcontainers:ubuntu/master Actions Status](https://github.com/akudiyar/testcontainers-java-module-tarantool/workflows/ubuntu-master/badge.svg)](https://github.com/akudiyar/testcontainers-java-module-tarantool/actions)

Testcontainers module for the [Tarantool](https://tarantool.io) database and application server and the [Tarantool Cartridge](https://tarantool.io/cartridge) framework.

See [testcontainers.org](https://www.testcontainers.org) for more information about TestContainers.

## Installation

Add the Maven dependency:

```xml
<dependency>
  <groupId>io.tarantool</groupId>
  <artifactId>testcontainers-java-tarantool</artifactId>
  <version>0.5.1</version>
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
        container.executeCommand("return 1, 2").get();
        // Or execute a script
        container.executeScript("org/testcontainers/containers/test.lua").get();
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
            List<Object> result = container.executeCommand("return 1, 2").get();
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
            // Point out the classpath-relative directory where the application files reside
            .withDirectoryBinding("cartridge")
            .withRouterHost("localhost") // Optional, "localhost" is default
            .withRouterPort(3301) // Binary port, optional, 3301 is default
            .withAPIPort(8801) // Cartridge HTTP API port, optional, 8081 is default
            .withRouterUsername("admin") // Specify the actual username, default is "admin"
            .withRouterPassword("testapp-cluster-cookie") // Specify the actual password, see the "cluster_cookie" parameter
                                                          // in the cartridge.cfg({...}) call in your application.
                                                          // Usually it can be found in the init.lua module
            .withReuse(true); // allows to reuse the container build once for faster testing

    // Use the created container in tests
    public void testFoo() {
        // Execute Lua commands in the router instance
        List<Object> result = container.executeCommand("return profile_get(...)", 1).get();

        // Instantiate a client connected to the router node
        TarantoolCredentials credentials = new SimpleTarantoolCredentials(getRouterUsername(), getRouterPassword());
        TarantoolServerAddress address = new TarantoolServerAddress(getRouterHost(), getRouterPort());
        TarantoolClientConfig config = TarantoolClientConfig.builder().withCredentials(credentials).build();
        try (ClusterTarantoolTupleClient client = new ClusterTarantoolTupleClient(config, address)) {
            // Do something with the client...
        }
    }
```

## License

See [LICENSE](LICENSE).

## Copyright

Copyright (c) 2020 Alexey Kuzin and other authors.

See [AUTHORS](AUTHORS) for contributors.
