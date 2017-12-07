Maven file list plugin
====

The plugin generates an xml or json file whose entries are individual files in a specified folder.

Usage:
----

| Name          | Type       | Default value                          | Description                                              |
| ------------- | ---------- | -------------------------------------- | -------------------------------------------------------- |
| baseDir       | String     | ${basedir}/target/.                    | Base directory of the scanning process                   |
| caseSensitive | boolean    |                                        | Whether to ignore case                                   |
| excludes      | String\[\] | none                                   | Ant-style exclude pattern. For example \*\*.\*           |
| includes      | String\[\] | all files recursive                    | Ant-style include pattern. For example \*\*.\*           |
| type          | String     | json                                   | json or xml                                              |
| fields        | Field\[\]  | { Field.name }                         | allowed: name, size, creationTime, lastModifiedTime      |
| outputFile    | String     | ${basedir}/target/file-list.{xml|json} | File into which to save the output of the transformation |

Original development by [NumberFour](https://github.com/NumberFour/jscoverage-cobertura-maven-plugin#readme)

Copyright [Banca d'Italia](www.bancaditalia.it), 2017.

Distributed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).
