FS2 Scalaz: Interoperability between FS2 and Scalaz
===============================================

[![Build Status](https://travis-ci.org/functional-streams-for-scala/fs2-scalaz.svg?branch=master)](http://travis-ci.org/functional-streams-for-scala/fs2-scalaz)
[![Gitter Chat](https://badges.gitter.im/functional-streams-for-scala/fs2.svg)](https://gitter.im/functional-streams-for-scala/fs2)

This library provides an interoperability layer between FS2 and Scalaz. At this time, the API of this library is two imports:

```scala
import fs2.interop.scalaz._         // Provides conversions from FS2 to Scalaz (e.g., FS2 Monad to Scalaz Monad)
                                    // as well as `Async` and `Async.Run` instances for Scalaz `Task`
import fs2.interop.scalaz.reverse._ // Provides conversions from Scalaz to FS2 (e.g., Scalaz Monad to FS2 Monad)
```

Note: importing both of these in to the same lexical scope may cause issues with ambiguous implicits.

### <a id="getit"></a> Where to get the latest version ###

```scala
// available for Scala 2.11.8 / 2.12.0-RC1 + Scalaz 7.2.6
libraryDependencies += "co.fs2" %% "fs2-scalaz" % "0.1.0"
```


