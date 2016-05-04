# Pipeline: a monadic workflow for Clojure apps.

Facilities for data transformation and validation in real-world Clojure applications.

## Installation

Add this to you `project.clj` or `build.boot`:
```clojure
[thdr.pipeline "0.1.0"]
```

## What's included?

+ A `pipeline->` (`>>=->`) and `pipeline->>` (`>>=->`) macros which work like Clojure's threading macros but thread an expression through a chain of monadic binds (`>>=`).
+ `thdr.pipeline.validations` namespace for working with data validation. Build on top of `Validation` applicative functor which works like `Either` monad, but can aggregate validation errors. Includes helpers for composing validations, validating associative data structures and transforming validations to `Either` monads.
+ `thdr.pipeline.schema` namespace for working with [Schema](https://github.com/plumatic/schema) checks and coercions within monadic context.
+ Various helpers to handle exceptions, eithers etc.

## Usage

I'm going to write some guides as soon as possible. For now check:

+ [Source code](https://github.com/konukhov/pipeline/tree/master/src/clj/thdr/pipeline/): I documented almost every function.
+ [Tests](https://github.com/konukhov/pipeline/tree/master/test/clj/thdr/pipeline/)
+ [Examples](https://github.com/konukhov/pipeline/tree/master/examples/)

## Notes

Built on top of [cats](https://github.com/funcool/cats) library which brings [Category Theory](https://en.wikipedia.org/wiki/Category_theory) concepts to Clojure.

## Contributing

Feel free to open an issue or PR :3

## License

Copyright © 2016 Theodore Konukhov <me@thdr.io>

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

