# Contents

1. [Introduction](#1-introduction)<br/>
2. [Limitations](#2-limitations)<br/>
3. [Requirements](#3-requirements)<br/>
4. [Approach](#4-approach)<br/>
4.1. [Formal Syntax](#41-formal-syntax)<br/>
4.2. [Expression Types](#42-expression-types)<br/>
4.2.1. [Constant](#421-constant)<br/>
4.2.2. [Synchronous](#422-synchronous)<br/>
4.2.3. [Asynchronous](#423-asynchronous)<br/>
4.3. [Initial Value](#43-initial-value)<br/>
4.4. [Built-in Functions](#44-built-in-functions)<br/>
4.4.1. [Random Path](#441-random-path)<br/>
4.5. [Built-in Values](#45-built-in-values)<br/>
4.5.1. [Self Reference](#451-self-reference)<br/>
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

Too meet the requirement #6 the whole input string should be a sequence of segments. Any segment may be a constant
string or an expression. The segment evaluation result is a constant string or an expression evaluation result. To
evaluate the value of the whole input the segments evaluation results are being concatenated sequentially.

In the EBNF notation:

```ebnf
SEGMENTS                  = SEGMENT*
SEGMENT                   = CONST_STRING | EXPRESSION
EXPRESSION                = (ASYNC_EXPR | SYNC_EXPR) \[ INIT_EXPR ]
ASYNC_EXPR                = "#" EXPR_BODY_WITH_BOUNDARIES
SYNC_EXPR                 = "$" EXPR_BODY_WITH_BOUNDARIES
CONST_EXPR                 = "%" EXPR_BODY_WITH_BOUNDARIES
EXPR_BODY_WITH_BOUNDARIES = "{" EXPR_BODY "}"
```

| Token          | Description |
|----------------|-------------|
| `CONST_STRING` | Any sequence of any symbols except `#{`/`${`/`%{` |
| `EXPR_BODY`    | The expression body which shouldn't contain `}` symbols neither any nested expressions |

For example the input string:
`prefix%{42}${this.last() + 1}%{pi * e}_#{date:formatNowIso8601}suffix`
will be split into the following sequence of segments:
1. `prefix`: constant string
2. `%{42}`: constant value expression
3. `${this.last() + 1}%{pi * e}`: synchronous expression with an initial value supplied by the constant value expression
4. `_`: constant string
5. `#{date:formatNowIso8601}`: asynchronous expression
6. `suffix`: constant string

The given input string will yield the result like:
`prefix428.539734_2019-03-07T15:23:46,461suffix`

## 4.2. Expression Types

### 4.2.1. Constant

The expression is being evaluated only once upon instantiation. It's specified by the marker `%`.

| Expression           | Yields |
|----------------------|--------|
| `%{42}`              | 42
| `%{rnd.nextInt(42)}` | A random integer in the range of \[0; 42)

The constant expression is useful also to supply a constant [initial value](#43-initial-value) for any other types of
expressions.

### 4.2.2. Synchronous

The expression is being evaluated on every access. The synchronous evaluation is useful if:
* The evaluation complexity is low enough
* The evaluation is non-blocking
* The different value on each invocation is strictly required

The synchronous expression is specified by the marker `$`:

`${time:millisSinceEpoch()}`

The expression above will yield different timestamp every time.

### 4.2.3. Asynchronous

The expression is being evaluated constantly in the background fiber. Requesting the expression value frequently is
expected to yield a sequence of the same value. The asynchronous evaluation is most useful when:
* The recalculation cost is too high
* The values consumer doesn't require different value each time

The synchronous expression is specified by the marker `#`:

`#{date:formatNowRfc1123()}`

The expression above will yield the date formatted using RFC1123 standard. The value will change sometimes irrespective
to the access.

**Warning**:
> The expression above may return `null` initially (before its 1st evaluation). To avoid this, set the initial value
> too: `#{date:formatNowRfc1123()}%{date:formatNowRfc1123()}`

## 4.3. Initial Value

The JUEL standard doesn't allow the initial value setting. However, this is required for the self-referencing
functionality. The constant value expression

`%{<INIT_EXPRESSION>}`

should be used right after an expression to set the initial value.

For example, the expression:

`${this.last() + 1}%{-1}`

will produce the following sequence of values: 0, 1, 2, ...

**Note**:
> The [self reference](#451-self-reference) is used in the example above to access the previous expression evaluation
> value

The expression may be used to supply an initial value:

`#{this.last() + 1}%{rnd.nextInt(42)}`

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
* [string:format(string pattern, args...)](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Formatter.html#format(java.lang.String,java.lang.Object...))
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

### 4.5.1. Self Reference

There are `this` among the built-in values. This is designed for the self referencing purposes. This allows to make an
expression evaluating the next value using the previous evaluation result. For example, the expression:

`${this.last() + 1}`

supplies the incremented value on each evaluation. The another example:

`${int64:xorShift(this.last())}`

supplies the new 64-bit random integer on each evaluation.

**Note**:
> * The only useful method for the `this` is [last](https://github.com/akurilov/java-commons/blob/a3cfeb4ed0985dc22832ce370b902de46f19062e/src/main/java/com/github/akurilov/commons/io/el/ExpressionInput.java#L35). Please don't use any other methods.

# 5. Configuration

## 5.1. Item Naming

There are two options responsible for the new items naming which support the expression values:

* `item-naming-prefix`
* `item-naming-seed`

*Note*:
> The `item-naming-seed` value may be an integer either a *non-composite* expression evaluating an integer value.

## 5.2. Variable Items Output Path

The `item-output-path` configuration option value may be an expression to generate the new path value for each new item
operation.

## 5.4. HTTP Request Headers And Queries

See the specific [HTTP storage driver documentation](storage/driver/coop/netty/http/README.md) for the details.

# 6. Future Enhancements

Use the expressions to define the following configuration options:
* `item-data-size`
* `item-data-ranges-bytes`
