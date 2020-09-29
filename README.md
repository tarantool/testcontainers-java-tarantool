# TestContainers Tarantool module

[![tarantool-testcontainers:ubuntu/master Actions Status](https://github.com/akudiyar/testcontainers-java-module-tarantool/workflows/ubuntu-master/badge.svg)](https://github.com/akudiyar/testcontainers-java-module-tarantool/actions)

Testcontainers module for the [Tarantool](https://tarantool.io) database and application server and the [Tarantool Cartridge](https://tarantool.io/cartridge) framework.

See [testcontainers.org](https://wwww.testcontainers.org) for more information about TestContainers.

## Installation

This module depends on the new asynchronous Tarantool driver:

```xml
<dependency>
  <groupId>io.tarantool</groupId>
  <artifactId>driver</artifactId>
</dependency>
```

Download and install it using `mvn install` from Github (a temporary measure until the first release is published to Maven Central).

Download and build this project via `mvn install` (a temporary measure until the first release is published to Maven Central).

Add the Maven dependency:

```xml
<dependency>
  <groupId>io.tarantool</groupId>
  <artifactId>testcontainers-java-tarantool</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Usage Example

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
        try (TarantoolClient client = new StandaloneTarantoolClient(credentials, serverAddress)) {
            Optional<TarantoolSpaceMetadata> spaceMetadata = client.metadata().getSpaceByName("test");
            ...

            // Execute some commands in Tarantool instance for verification
            List<Object> result = container.executeCommand("return 1, 2").get();
            ...
        }
    ...
```

## License

See [LICENSE](LICENSE).

## Copyright

Copyright (c) 2020 Alexey Kuzin and other authors.

See [AUTHORS](AUTHORS) for contributors.
