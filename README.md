# sbt-hocon
The sbt plugin intended to perform a number of utility operations with [Typesafe config](https://github.com/typesafehub/config) data.

## Features
 * Generate full reference configuration discovered from project dependencies.
 * Purify custom HOCON files by removing settings coniciding with defaults, 
   supplying with comments taken from reference configuration, 
   formatting with proper structure and indentation. 

## Usage
Add the following lines to `project/plugins.sbt`. See the section [Using Plugins](http://www.scala-sbt.org/release/tutorial/Using-Plugins.html) in the sbt wiki for more information.
```scala
resolvers += Resolver.sonatypeRepo("releases")
addSbtPlugin("com.github.aalleexxeeii" % "sbt-hocon" % "0.1.3")
```
## Tasks
### hoconDefaults
This task collects all reference configurations from project compile dependencies and produces a singe HOCON file from their combination.
    
    hoconDefaults [<output>]
If `-` is specified as `<output>`, the result will be sent to the standard output stream.

### hoconPurify
This task reads configuration from file `<input>`, compares it with defaults discovered from project dependencies,
removes settings with values no different from defaults and dumps the result to `<output>` file.

    hoconPurify <input> <output>
`-` can be specified instead of file paths to work with the standard input and output streams respectively.

## Keys
 * `hoconExtraResources` — additional reference files to consider (`Seq[String]]`)
