# Nano Framework

_(under construction)_

[//]: # ([![Build][build_shield]][build_link])

[//]: # ([![Maintainable][maintainable_shield]][maintainable_link])

[//]: # ([![Coverage][coverage_shield]][coverage_link])
[![Issues][issues_shield]][issues_link]
[![Commit][commit_shield]][commit_link]
[![License][license_shield]][license_link]
[![Central][central_shield]][central_link]
[![Tag][tag_shield]][tag_link]
[![Javadoc][javadoc_shield]][javadoc_link]
[![Size][size_shield]][size_shield]
![Label][label_shield]
![Label][java_version]

## 🚀 Elevate Your Java Experience

Welcome to **Nano Framework**, where simplicity meets power in Java development. Nano is designed for developers who
value a **lightweight**, **efficient**, and **straightforward** approach to building applications. It's the essence of
modern Java, stripped of complexity and enriched with functionality.

## 🧭 Navigation

* 🔖 [Philosophy](#-philosophy-back-to-basics)
* 🤔 [Why Nano](#-why-nano-the-nano-advantage)
* ✨ [Benefits](#-benefits-of-nano)
* 📚 [Getting Started](#-getting-started)
* 🔬 [Examples](#-examples)
* 🤝 [Contributing](#-contributing)
* 📜 [License](#-license)
* 🙋‍ [Support](#-support)
* 🌐 [Stay connected](#-stay-connected)

## 🔖 Philosophy: Back to Basics

Nano was born from the desire to simplify Java development. It's about going back to basics and harnessing the power of
Java without the baggage of heavy frameworks. Nano is for those who believe in keeping things simple, efficient, and
sustainable.

## 🤔 Why Nano? The Nano Advantage

Serverless & Container Ready: Tailored for modern deployment environments.
Small & Performant: Ideal for resource-constrained environments.
Security Conscious: Minimized attack surface due to few dependencies.
Clear & Cohesive: No scattered logic, everything in its right place.
Eco-Friendly Development: Less resource usage means lower costs and energy consumption.

## 📚 Getting Started

_(under construction)_
To get started with Nano, simply include it in your Java project. You can build it from the source or include it as a
dependency in your build management tool.

## 📟 Quick Start

_(under construction)_
Here's a quick example to demonstrate the ease of using Nano:

```java
import de.yuna.berlin.nativeapp.core.Nano;

public class UsageExample {

    public static void main(final String[] args) {
        final Nano application = new Nano(new LogQueue(), new HttpService(8080), args);

        // some post-processing
        final Context context = application.context(UsageExample.class)
            .async(new ServiceA())
            .asyncAwait(new ServiceB())
            .sendEvent(EVENT_APP_START.id(), null)
            .schedule(() -> context.sendEvent(EVENT_APP_SHUTDOWN.id(), null), 10, TimeUnit.SECONDS);
    }
}
```

This snippet showcases the creation of a Nano application, service registration, asynchronous task execution, and event
handling.

## ✨ Benefits of Nano:

* ✅ **Modular Design**: Nano's architecture is modular, making it easy to understand, extend, and maintain.
* ✅ **Concurrency Management**: Efficiently handle asynchronous tasks using advanced thread management.
* ✅ **Event-Driven Architecture**: Robust event handling that simplifies communication between different parts of your
  application.
* ✅ **Flexible Configuration**: Configure your application using environment variables, system properties, or
  command-line
  arguments.
* ✅ **Robust Logging and Error Handling**: Integrated logging and comprehensive error handling mechanisms for reliable
  operation.
* ✅ **Scalable and Performant**: Designed with scalability and performance in mind to handle high-concurrency scenarios.
* ✅ **Lightweight & Fast**: Starts in milliseconds, uses ~10MB memory.
* ✅ **Pure Java, Pure Simplicity**: No reflections, no regex, no unnecessary magic.
* ✅ **GraalVM Ready**: For ahead-of-time compilation and faster startup.
* ✅ **Minimal Dependencies**: Reduces CVE risks and simplifies updates.
* ✅ **Fluent & Stateless**: Intuitive API design for easy readability and maintenance.
* ✅ **Rapid Service Development**: Build real services in minutes.

# TODO: complete following:

## 🔬 Examples

_(under construction)_

Check out various examples demonstrating different capabilities of Nano here.

## 🤝 Contributing

Contributions to Nano are welcome! Please refer to our [Contribution Guidelines](CONTRIBUTING.md) for more information.

## 📜 License

Nano is open-source software licensed under the [Apache license](LICENSE).

## 🙋‍ Support

If you encounter any issues or have questions, please file an
issue [here](https://github.com/YunaBraska/nano/issues/new/choose).

## 🌐 Stay Connected

* [GitHub](https://github.com/YunaBraska)
* [X (aka Twitter)](https://twitter.com/YunaMorgenstern)
* [Mastodon](https://hachyderm.io/@LunaFreyja)
* [LinkedIn](https://www.linkedin.com/in/yuna-morgenstern-6662a5145/)

![tiny_java_logo](src/test/resources/tiny_java.png)


[build_shield]: https://github.com/YunaBraska/nano/workflows/MVN_RELEASE/badge.svg

[build_link]: https://github.com/YunaBraska/nano/actions?query=workflow%3AMVN_RELEASE

[maintainable_shield]: https://img.shields.io/codeclimate/maintainability/YunaBraska/nano?style=flat-square

[maintainable_link]: https://codeclimate.com/github/YunaBraska/nano/maintainability

[coverage_shield]: https://img.shields.io/codeclimate/coverage/YunaBraska/nano?style=flat-square

[coverage_link]: https://codeclimate.com/github/YunaBraska/nano/test_coverage

[issues_shield]: https://img.shields.io/github/issues/YunaBraska/nano?style=flat-square

[issues_link]: https://github.com/YunaBraska/nano/issues/new/choose

[commit_shield]: https://img.shields.io/github/last-commit/YunaBraska/nano?style=flat-square

[commit_link]: https://github.com/YunaBraska/nano/commits/main

[license_shield]: https://img.shields.io/github/license/YunaBraska/nano?style=flat-square

[license_link]: https://github.com/YunaBraska/nano/blob/main/LICENSE

[dependency_shield]: https://img.shields.io/librariesio/github/YunaBraska/nano?style=flat-square

[dependency_link]: https://libraries.io/github/YunaBraska/nano

[central_shield]: https://img.shields.io/maven-central/v/berlin.yuna/nano?style=flat-square

[central_link]:https://search.maven.org/artifact/berlin.yuna/nano

[tag_shield]: https://img.shields.io/github/v/tag/YunaBraska/nano?style=flat-square

[tag_link]: https://github.com/YunaBraska/nano/releases

[javadoc_shield]: https://javadoc.io/badge2/berlin.yuna/nano/javadoc.svg?style=flat-square

[javadoc_link]: https://javadoc.io/doc/berlin.yuna/nano

[size_shield]: https://img.shields.io/github/repo-size/YunaBraska/nano?style=flat-square

[label_shield]: https://img.shields.io/badge/Yuna-QueenInside-blueviolet?style=flat-square

[gitter_shield]: https://img.shields.io/gitter/room/YunaBraska/nano?style=flat-square

[gitter_link]: https://gitter.im/nano/Lobby

[java_version]: https://img.shields.io/badge/java-21-blueviolet?style=flat-square
