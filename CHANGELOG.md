# Changelog

## [Unreleased]

## [1.1.1] - 2023-12-13

- Change private to protected in TarantoolCartridgeContainer

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
