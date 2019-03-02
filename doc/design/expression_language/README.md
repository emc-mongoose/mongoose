# Contents

TODO

# 1. Introduction

Mongoose is extensible with storage driver and load step plugins. It's also supports scenarios scripting with any
JSR-223 compatible language. Moreover, any Mongoose plugin may introduce its own configuration options which are being
dynamically embedded into the runtime configuration (the modular configuration feature). It is also very useful to
parameterize some configuration options, i.e. the given option should supply some dynamically evaluated value which may
be different on each value read. The new Mongoose version provides a general mechanism to describe the dynamic
configuration options based on [Java Unified Expression Language](http://juel.sourceforge.net/index.html)
([JSR-341](https://github.com/javaee/el-spec/blob/master/spec/SATCK%20JSR%20341%20Expression%20Language%203.0%202.20.13.pdf)).

# 2. Limitations

# 3. Requirements

# 4. Approach

## 4.1. Built-in Functions

* [env:get(String name)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/System.html#getenv(java.lang.String))
* [int64:toString(long x, int radix)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#toString(long,int))
* [int64:toUnsignedString(long x, int radix)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#toUnsignedString(long,int))
* [int64:reverse(long x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#reverse(long))
* [int64:reverseBytes(long x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#reverseBytes(long))
* [int64:rotateLeft(long x, int distance)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#rotateLeft(long,int))
* [int64:rotateRight(long x, int distance)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#rotateRight(long,int))
* [math:absInt32(int x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#abs(int))
* [math:absInt64(long x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#abs(long))
* [math:absFloat32(float x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#abs(float))
* [math:absFloat64(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#abs(double))
* [math:acos(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#acos(double))
* [math:asin(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#asin(double))
* [math:atan(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#atan(double))
* [math:ceil(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#ceil(double))
* [math:cos(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#cos(double))
* [math:exp(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#exp(double))
* [math:floor(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#floor(double))
* [math:log(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#log(double))
* [math:log10(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#log10(double))
* [math:maxInt32(int x1, int x2)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#max(int,int))
* [math:maxInt64((long x1, long x2)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#max(long,long))
* [math:maxFloat32(float x1, float x2)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#max(float,float))
* [math:maxFloat64(double x1, double x2)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#max(double,double))
* [math:minInt32(int x1, int x2)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#min(int,int))
* [math:minInt64((long x1, long x2)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#min(long,long))
* [math:minFloat32(float x1, float x2)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#min(float,float))
* [math:minFloat64(double x1, double x2)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#min(double,double))
* [math:pow(double a, double b)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#pow(double,double))
* [math:sin(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#sin(double))
* [math:sqrt(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#sqrt(double))
* [math:tan(double x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Math.html#tan(double))
* [math:xorShift64(long x)](https://github.com/akurilov/java-commons/blob/a3cfeb4ed0985dc22832ce370b902de46f19062e/src/main/java/com/github/akurilov/commons/math/MathUtil.java#L34)
* [string:format(string pattern, args...)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#format(java.lang.String,java.lang.Object...))
* [string:join(string delimeter, string elements...)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#join(java.lang.CharSequence,java.lang.CharSequence...))
* [time:millisSinceEpoch()](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/System.html#currentTimeMillis())
* [time:nanos()](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/System.html#nanoTime())

## 4.2. Built-in Values

| Name | Type | Description |
|------|------|-------------|
| `e` | double | The base of natural logarithms
| `lineSep` | string | The system-dependent line separator string
| `pathSep` | string | The system-dependent path-separator
| `pi` | double | The ratio of the circumference of a circle to its diameter
| `this` | [ExpressionInput](https://github.com/akurilov/java-commons/blob/master/src/main/java/com/github/akurilov/commons/io/el/ExpressionInput.java) | The expression input instance (self referencing)

### 4.2.1. Self Referencing

There are `this` among the built-in values. This is designed for the self referencing purposes. This allows to make an
expression evaluating the next value using the previous evaluation result. For example, the expression:
```${this.last() + 1}```
supplies the incremented value on each evaluation. The another example:
```${math:xorShift64(this.last())}```
supplies the new 64-bit random integer on each evaluation.

**Note**:
> * The initial value should be set if the self referencing is used.
> * The only useful method for the `this` is [last](https://github.com/akurilov/java-commons/blob/a3cfeb4ed0985dc22832ce370b902de46f19062e/src/main/java/com/github/akurilov/commons/io/el/ExpressionInput.java#L35). Please don't use any other methods.

# 5. Configuration

List of the options, supporting the expression values:
* `item-id`
* `item-name`
* `item-output-path`
* `storage-auth-uid`
* `storage-auth-secret`
* `storage-net-http-headers-*`
* `storage-net-http-uri-args-*`

# 6. Future Enhancements

Use the expressions to define the following configuration options:
* `item-data-size`
* `item-data-ranges-bytes`
