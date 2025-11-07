# Changelog

## [1.4.1] - 2025-11-07
- Bump httpclient from 4.5.14 to httpclient5 5.5.1
- Bump various maven plugins

## [1.4.0] - 2025-07-24
- Bump testcontainers.version from 1.19.3 to 1.21.3
- Bump jackson-annotations from 2.14.0 to 2.18.4
- Bump slf4j-api from 2.0.3 to 2.0.17
- Bump snakeyaml from 2.0 to 2.3
- Bump httpclient from 4.5.1 to 4.5.14
- Bump logback-classic from 1.3.4 to 1.3.15
- Bump junit-jupiter from 5.8.1 to 5.13.3

## [1.3.3] - 2024-05-06
- Add support for environment variable `TARANTOOL_REGISTRY`
- Remove enterprise tests

## [1.3.2] - 2024-04-25
- Fix NullPointerException during building docker image when images with null tags exist on a local machine.

## [1.3.1] - 2024-02-13

- Change `TARANTOOL_VERSION` default value from `2.11.2-centos7` to `2.11.2-ubuntu20.04`. It ruins rocks building due to certificate issues on centos.

## [1.3.0] - 2024-02-02

- Change `TARANTOOL_VERSION` semantic. Now it's expected to be a full tag, not prefix of centos version.
  ```
    was: ${TARANTOOL_VERSION}-centos7 # TARANTOOL_VERSION == 2.11.2
    now: ${TARANTOOL_VERSION} # TARANTOOL_VERSION could be == 2.11.2-centos7
  ```
  ([#114](https://github.com/tarantool/testcontainers-java-tarantool/pull/114))
- Support executeCommand for tarantool 3.0+
  

## [1.2.0] - 2023-12-14

- Remove the default value for `TARANTOOL_CLUSTER_COOKIE` env variable in cartridge container.
  Now it works like in cartridge in order of decreasing priority as directed by the user:
  `TARANTOOL_CLUSTER_COOKIE > cartridge.cfg > default_cookie`
  You can set TARANTOOL_CLUSTER_COOKIE by build-arg on image building or by env arg before container starting
  ([#55](https://github.com/tarantool/testcontainers-java-tarantool/issues/55))
- **[Breaking change]** Default routerPassword has been changed from `testapp-cluster-cookie` to `secret-cluster-cookie`
  ([#55](https://github.com/tarantool/testcontainers-java-tarantool/issues/55))
- Change private modifier to protected modifier for fields and methods in TarantoolCartridgeContainer
- Add `TARANTOOL_VERSION` environment variable support to TarantoolCartridgeContainer
  `tarantool/tarantool:<TARANTOOL_VERSION>-centos7` if image name is omitted
  ([#102](https://github.com/tarantool/testcontainers-java-tarantool/pull/102))

## [1.1.0] - 2023-12-12

- Bump testcontainers to 1.19.3 ([#99](https://github.com/tarantool/testcontainers-java-tarantool/issues/99))
- Remove cached rocks on cartridge image rebuilding ([#99](https://github.com/tarantool/testcontainers-java-tarantool/issues/99))
- Add passing instancesFile to cartridge replicasets command ([#99](https://github.com/tarantool/testcontainers-java-tarantool/issues/99))

## [1.0.2] - 2023-11-10

- Make a separate step for building the cartridge ([#94](https://github.com/tarantool/testcontainers-java-tarantool/issues/94))

## [1.0.1] - 2023-10-04

- Fix command template for use with the 1.x Tarantool version ([#87](https://github.com/tarantool/testcontainers-java-tarantool/issues/87))
- Propagate log directory to cartridge-cli command ([#83](https://github.com/tarantool/testcontainers-java-tarantool/issues/83))

## [1.0.0] - 2023-06-16
- Fix problem if topology isn't applied correctly
- Bump testcontainers to 1.18.0
- Move rocks building in build phase
- Use "addExposedPorts" instead of "withExposedPorts"
- **[breaking change]** Remove io.tarantool.cartridge-driver dependency
- **[breaking change]** Update executeScript and executeCommand methods to execute code via execInContainer  
  (now it returns **yaml** string in Container.ExecResult instead of CompletableFuture)
- Add executeScriptDecoded and executeCommandDecoded methods to return parsed yaml not string
- Add SslContext class
- Add withSslContext method to TarantoolContainer and TarantoolCartridgeContainer
- Update org.yaml.snakeyaml to 2.0 version

## [0.5.4] - 2023-03-31
- Use tarantool image as base instead of centos in cartridge container

## [0.5.3] - 2022-11-14
- Bump logback-classic to 1.3.4 to fix logging
- Bump slf4j-api to 2.0.3
- Remove redundant junit-jupiter dependency

## [0.5.2] - 2022-11-11
- Add dependency management
- Bump snakeyaml to 1.33
- Bump junit-jupiter to 5.9.0
- Bump logback-classic to 1.2.11

## [0.5.1] - 2022-10-28
- Bump cartridge-java to 0.9.1
- Bump testcontainers to 1.17.4

## [0.5.0] - 2022-05-18
- Added ability to configure cluster from yaml (#40)
- Added migrations example in tests (#42)
- Added feature for creating tarantool container from sdk (#43)

## [0.4.7] - 2022-03-24
 - Fixed handling of instances parameter (can be passed via TARANTOOL_INSTANCES_FILE)
 - Updated tarantool library dependencies
