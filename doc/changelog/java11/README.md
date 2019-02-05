# Migration to Java 11

## Motivation

Mongoose had been using Java 8 for both build environment and runtime for a long time.
However, Java 8 is getting close to its [EOL](https://www.oracle.com/technetwork/java/java-se-support-roadmap.html). On the other hand, the new Java versions include
the features which are very useful for the developers and users. Few examples:

| JEP # | Application |
|---|---|
| 282 | Allows to build the custom JRE without uneccessary components and downsize the resulting distributable image
| 286 | Allows the developers to produce cleaner code
| 307 | GC performance improvement
| 312 | Performance improvement

# Performance

![](../../images/driver_tp_java_8vs11.png)

![](../../images/create_perf_java_8vs11.png)
