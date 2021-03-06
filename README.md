# sbt-hocon
The sbt plugin intended to perform a number of utility operations with [Typesafe config](https://github.com/typesafehub/config) data.

## Features
 * Generate full reference configuration discovered from project dependencies.
 * Purify custom HOCON files by removing settings coniciding with defaults, 
   supplying with comments taken from reference configuration, 
   formatting with proper structure and indentation. 

## Usage
Add the following lines to `project/plugins.sbt`. See the section [Using Plugins](http://www.scala-sbt.org/release/docs/Using-Plugins.html) in the sbt wiki for more information.
```scala
addSbtPlugin("com.github.aalleexxeeii" % "sbt-hocon" % "0.1.7")
```
## Tasks
### hoconDefaults
This task collects all reference configurations from project compile dependencies and produces a singe HOCON file from their combination.
    
    hoconDefaults [options] [<output>]
If `@` is specified as `<output>`, the result will be sent to the standard output stream.

### hoconPurify
This task reads configuration from file `<input>`, compares it with defaults discovered from project dependencies,
removes settings with values no different from defaults and dumps the result to `<output>` file.

    hoconPurify [options] <input> <output>
`@` can be specified instead of file paths to work with the standard input and output streams respectively.

## Keys
 * `hoconExtraResources` — additional reference files to consider (`Seq[String]]`)

# Options
A number of options can be specified for every task to customize beghavior.
 
 * `-c, --comment <mode>` — Mode for comments: 
   * `off` - no comments; 
   * `override` - use top-level comment (default); 
   * `merge` - merge all comments together. 
 * `--origin-comments` — Include origin in comments
 * `-i, --include <path1>[,<path2>...]` — Include just the given paths
 * `-x, --exclude <path1>[,<path2>...]` — Exclude the given paths

# License
This software is under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
