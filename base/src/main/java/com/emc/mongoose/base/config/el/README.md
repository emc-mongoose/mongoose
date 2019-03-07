# Contents

1. [Introduction](#1-introduction)<br/>
2. [Limitations](#2-limitations)<br/>
3. [Requirements](#3-requirements)<br/>
4. [Approach](#4-approach)<br/>
4.1. [Synchronous And Asynchronous Evaluation](#41-synchronous-and-asynchronous-evaluation)<br/>
4.2. [Initial Value](#42-initial-value)<br/>
4.3. [Types Summary](#43-types-summary)<br/>
4.4. [Built-in Functions](#44-built-in-functions)<br/>
4.4.1. [Random Path](#441-random-path)<br/>
4.5. [Built-in Values](#45-built-in-values)<br/>
4.5.1. [Self-referencing](#451-self-referencing)<br/>
5. [Configuration](#5-configuration)<br/>
5.1. [Variable Items Output Path](#51-variable-items-output-path)<br/>
5.2. [HTTP request headers and queries](#52-http-request-headers-and-queries)<br/>
6. [Future Enhancements](#6-future-enhancements)

# 1. Introduction

Mongoose is extensible with storage driver and load step plugins. It also supports scenarios scripting with any JSR-223
compatible language. Moreover, any Mongoose plugin may introduce its own configuration options which are being
dynamically embedded into the runtime configuration (the modular configuration feature). It is also very useful to
parameterize some configuration options, i.e. the given option should supply some dynamically evaluated value which may
be different on each value read. The new Mongoose version provides a general mechanism to describe the dynamic
configuration options based on [Java Unified Expression Language](http://juel.sourceforge.net/index.html)
([JSR-341](https://github.com/javaee/el-spec/blob/master/spec/SATCK%20JSR%20341%20Expression%20Language%203.0%202.20.13.pdf)).

# 2. Limitations

1. JUEL standard doesn't allow to mix the
   [synchronous and asynchronous evaluation](#41-synchronous-and-asynchronous-evaluation) in the same expression
2. The [initial value](#42-initial-value) should be set if the [self referencing](#431-self-referencing) is used

# 3. Requirements

1. Support both synchronous and asynchronous expression evaluation
2. The expression result should be refreshing in background constantly in case of asynchronous evaluation
3. The JUEL syntax should be extended to support the initial/seed value setting
4. The expression should be able to use the result of the previous evaluation (self referencing)
5. The expression should be able to invoke custom functions and access custom variables
6. The expression should be able to consist of parts: prefixes/suffixes and multiple independent expressions

# 4. Approach

The [requirements 2-5](#3-requirements) are implemented as the
[JUEL extension in the *java-commons* library](https://github.com/akurilov/java-commons/blob/master/src/test/java/com/github/akurilov/commons/io/el/ExpressionInputTest.java).

## 4.1. Formal Syntax

BNF notation:
```ebnf
SEGMENTS ::= SEGMENT*
SEGMENT ::= CONSTANT_STRING | EXPRESSION
EXPRESSION := (ASYNC_EXPRESSION | SYNC_EXPRESSION) \[ INIT_EXPRESSION ]
ASYNC_EXPRESSION := "#" EXPRESSION_BODY_WITH_BOUNDARIES
SYNC_EXPRESSION := "$" EXPRESSION_BODY_WITH_BOUNDARIES
INIT_EXPRESSION := "%" EXPRESSION_BODY_WITH_BOUNDARIES
EXPRESSION_BODY_WITH_BOUNDARIES := "{" EXPRESSION_BODY "}"
```

## 4.1. Synchronous And Asynchronous Evaluation

JUEL supports both immediate (synchronous) and deferred (asynchronous) evaluation ways.

* **Synchronous**
The expression is being evaluated on every invocation. The synchronous evaluation is useful if:
    * The evaluation complexity is low enough
    * The evaluation is non-blocking
    * The different value on each invocation is strictly required.
* **Asynchronous**
The expression is being evaluated constantly in the background fiber. Requesting the expression value frequently is
expected to yield a sequence of the same value. The asynchronous evaluation is most useful when:
    * The recalculation cost is too high
    * The values consumer doesn't require different value each time

The symbols `$` (synchronous) and `#` (async) are used in the JUEL standard to distinguish between the synchronous and
asynchronous evaluation.

## 4.2. Initial Value

The JUEL standard doesn't allow the initial value setting. However, this is required for the self-referencing
functionality. The pattern

`%{<INIT_EXPRESSION>}`

should be used right before the expression to set the initial value.

For example, the expression:

`%{-1}${this.last() + 1}`

will produce the following sequence of values: 0, 1, 2, ...

### 4.2.1. Initial Value Expression

The useful thing is that the initial value is also an expression which is being evaluated *once* to provide the
constant initial value:

`%{rnd.nextInt(42)}#{this.last() + 1}`

### 4.2.2. Constant Value Expression

Some expressions actually are being evaluated only once and then the resulting value is reused without changes:

`%{rnd.nextInt(42)}${this.last()}`

For performance considerations such expression shouldn't be evaluated every time to get a constant value. To do this,
the following specific syntax should be used:

`%{rnd.nextInt(42)}`

## 4.3. Types Summary

| Marker | Evaluation                    | May be used as initial value expression for another one
|--------|-------------------------------|-------|
| `$`    | On every access (synchronous) | false |
| `#`    | In the background (async)     | false |
| `%`    | Once only                     | true  |

## 4.4. Built-in Functions

There are some useful static Java methods mapped into the expression language:
* date:formatNowIso8601()
* date:formatNowRfc1123()
* [env:get(String name)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/System.html#getenv(java.lang.String))
* [int64:toString(long x, int radix)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#toString(long,int))
* [int64:toUnsignedString(long x, int radix)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#toUnsignedString(long,int))
* [int64:reverse(long x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#reverse(long))
* [int64:reverseBytes(long x)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#reverseBytes(long))
* [int64:rotateLeft(long x, int distance)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#rotateLeft(long,int))
* [int64:rotateRight(long x, int distance)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Long.html#rotateRight(long,int))
* int64:xor(long x1, long x2)
* [int64:xorShift(long x)](https://github.com/akurilov/java-commons/blob/a3cfeb4ed0985dc22832ce370b902de46f19062e/src/main/java/com/github/akurilov/commons/math/MathUtil.java#L34)
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
* [path:random(int width, int depth)](#431-random-path-generator)
* [string:format(string pattern, args...)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#format(java.lang.String,java.lang.Object...))
* [string:join(string delimeter, string elements...)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#join(java.lang.CharSequence,java.lang.CharSequence...))
* [time:millisSinceEpoch()](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/System.html#currentTimeMillis())
* [time:nanos()](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/System.html#nanoTime())

### 4.4.1. Random Path

The random path function yields a random path specified by ***width*** and ***depth*** parameters, where the width
specifies the maximum count of the directories per one level and depth specifies the maximum count of such levels.

## 4.5. Built-in Values

| Name | Type | Description |
|------|------|-------------|
| `e` | double | The base of natural logarithms
| `lineSep` | string | The system-dependent line separator string
| `pathSep` | string | The system-dependent path-separator
| `pi` | double | The ratio of the circumference of a circle to its diameter
| `rnd` | Random | The random number generator
| `this` | [ExpressionInput](https://github.com/akurilov/java-commons/blob/master/src/main/java/com/github/akurilov/commons/io/el/ExpressionInput.java) | The expression input instance (self referencing)

### 4.5.1. Self Referencing

There are `this` among the built-in values. This is designed for the self referencing purposes. This allows to make an
expression evaluating the next value using the previous evaluation result. For example, the expression:

`${this.last() + 1}`

supplies the incremented value on each evaluation. The another example:

`${int64:xorShift(this.last())}`

supplies the new 64-bit random integer on each evaluation.

**Note**:
> * The only useful method for the `this` is [last](https://github.com/akurilov/java-commons/blob/a3cfeb4ed0985dc22832ce370b902de46f19062e/src/main/java/com/github/akurilov/commons/io/el/ExpressionInput.java#L35). Please don't use any other methods.

# 5. Configuration

## 5.1. Variable Items Output Path

The `item-output-path` configuration option value may be an expression to generate the new path value for each new item
operation.

## 5.2. HTTP Request Headers And Queries

See the specific [HTTP storage driver documentation](storage/driver/coop/netty/http/README.md) for the details.

# 6. Future Enhancements

Use the expressions to define the following configuration options:
* `item-data-size`
* `item-data-ranges-bytes`
