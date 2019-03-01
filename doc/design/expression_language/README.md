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

# 5. Configuration

List of the options, supporting the expressions:
* `item-data-size`
*

# 6. Future Enhancements
