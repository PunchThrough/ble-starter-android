# Punch Through Android BLE Starter App

[![CircleCI build status](https://circleci.com/gh/PunchThrough/ble-starter-android/tree/master.svg?style=svg)](https://circleci.com/gh/PunchThrough/ble-starter-android/tree/master)

Companion Android app project for [Punch Through](https://punchthrough.com)'s "Ultimate Guide to Android BLE Development" blog post for beginners, with examples of how to perform basic BLE operations and some Android BLE tips and tricks on the following:

- Scanning for nearby BLE devices
- Connecting to BLE devices
- Discovering services and characteristics
- Requesting an ATT MTU update
- Reading and writing data on characteristics and descriptors
- Enabling and disabling notifications and indications on characteristics
- Bonding with a BLE device
- Implementing your own BLE operations serial queuing mechanism

## Setup

1. Clone the project to your directory of choice.

```
git clone https://github.com/PunchThrough/ble-starter-android.git
```

2. Launch Android Studio and select "Open an existing Android Studio project".
3. Navigate to the directory where you cloned the project to, and double click on it.
4. Wait for Gradle sync to complete.

## Requirements

This project targets Android 10 and has a min SDK requirement of 21 (Android 5.0), in line with our recommendation in [4 Tips to Make Android BLE Actually Work](https://punchthrough.com/android-ble-development-tips/).

## Contributing

### Reporting bugs

Please [open an issue](https://github.com/PunchThrough/ble-starter-android/issues/new) to report a bug if the app isn't behaving as expected.

### Opening a Pull Request

Please fork the repository and create a feature branch before opening a Pull Request against the `master` branch.

### Linting and code style

The project uses Kotlin's default [coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html) and includes the `.idea/codeStyle` directory in source control. The project also runs [`ktlint`](https://ktlint.github.io) as part of the CI process to ensure code consistency.

You may run `ktlint` locally using the following command:

```
./gradlew ktlint
```

Some simpler violations can be automatically formatted by `ktlint` using the following command:

```
./gradlew ktlintFormat
```

## Licensing

This project is licensed under the Apache 2.0. For more details, please see [LICENSE](https://github.com/PunchThrough/ble-starter-android/blob/master/LICENSE).
