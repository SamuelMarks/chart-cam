fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

### test_all

```sh
[bundle exec] fastlane test_all
```



----


## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android build

```sh
[bundle exec] fastlane android build
```

Builds the release bundle

### android deploy_internal

```sh
[bundle exec] fastlane android deploy_internal
```

Submit a new Internal Build to Play Store

----


## iOS

### ios test

```sh
[bundle exec] fastlane ios test
```

Runs all the tests

### ios build

```sh
[bundle exec] fastlane ios build
```

Builds the app for release

### ios deploy_testflight

```sh
[bundle exec] fastlane ios deploy_testflight
```

Push a new beta build to TestFlight

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
