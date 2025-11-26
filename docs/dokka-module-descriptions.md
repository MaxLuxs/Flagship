# Flagship Documentation

**Flagship** is a powerful, type-safe feature flags and A/B testing library for Kotlin Multiplatform.

## Modules

### flagship-core
Core module containing the primary API, evaluation engine, and cache system. Provides the main `Flagship` object, `FlagsManager` interface, and all core models and utilities.


### flagship-provider-rest
REST API provider implementation. Allows fetching flags and experiments from any REST API endpoint. Supports custom JSON schemas and automatic snapshot parsing.

### flagship-provider-firebase
Firebase Remote Config provider implementation. Seamless integration with Firebase Remote Config for Android and iOS. Provides factory methods for easy setup.

### flagship-ui-compose
Compose Multiplatform UI components for debugging and testing flags. Includes `FlagsDashboard` composable for inspecting flags, setting overrides, and testing experiments locally.

### flagship-codegen
Gradle plugin for generating type-safe flag classes from JSON configuration. Automatically generates Kotlin code from `flags.json` files, providing compile-time safety and autocomplete.

### flagship-provider-launchdarkly
LaunchDarkly provider implementation. Allows using Flagship with LaunchDarkly as a backend provider.

### flagship-ktor-plugin
Ktor server plugin for serving flags and experiments. Enables building custom flag management servers with Ktor.

### flagship-spring-boot-starter
Spring Boot starter for Flagship server integration. Provides auto-configuration and easy setup for Spring Boot applications.

