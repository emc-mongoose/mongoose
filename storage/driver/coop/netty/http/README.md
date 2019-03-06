# HTTP Storage Drivers

## 1. Configuration Reference

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| storage-net-http-headers                       | Map          | { "Connection" : "keep-alive", "Date": "%{date:formatNowRfc1123()}#{date:formatNowRfc1123()}", "User-Agent" : "mongoose/4.0.2" } | Custom HTTP headers section. An user may place here a key-value pair which will be used as HTTP header. The headers will be appended to every HTTP request issued.
| storage-net-http-uri-args                      | Map          | {}               | Custom URI query arguments according [RFC 2396](http://www.ietf.org/rfc/rfc2396.txt).The headers will be appended to every HTTP request issued.

## 2. Custom HTTP Headers

Scenario example:
```javascript
var customHttpHeadersConfig = {
    "storage" : {
        "net" : {
            "http" : {
                "headers" : {
                    "header-name-0" : "header_value_0",
                    "header-name-1" : "header_value_1",
                    // ...
                    "header-name-N" : "header_value_N"
                }
            }
        }
    }
};

Load
    .config(customHttpHeadersConfig)
    .run();
```

**Note**:
> Don't use the command line arguments for the custom HTTP headers setting.

### 2.1. Expressions

Scenario example, note both the parameterized header name and value:
```javascript
var varHttpHeadersConfig = {
    "storage" : {
        "net" : {
            "http" : {
                "headers" : {
                    "x-amz-meta-${math:random(30) + 1}" : "${date:format("yyyy-MM-dd'T'HH:mm:ssZ").format(date:from(rnd.nextLong(time:millisSinceEpoch())))}"
                }
            }
        }
    }
};

Load
    .config(varHttpHeadersConfig)
    .run();
```

## 3. Custom URI Arguments

Custom URI query arguments may be set in the same way as custom HTTP headers.

```javascript
var uriQueryConfig = {
    "storage" : {
        "net" : {
            "http" : {
                "uri" : {
                    "args" : {
                        "foo": "bar",
                        "key1" : "val1"
                    }
                }
            }
        }
    }
};

Load
    .config(uriQueryConfig)
    .run();
```

will produce the HTTP requests with URIs like:
`/20190306.104255.627/kticoxcknpuy?key1=val1&foo=bar`


**Note**:
> Don't use the command line arguments for the custom HTTP URI query arguments setting.

### 3.1. Expressions

Example:
```javascript
var uriQueryConfig = {
    "storage" : {
        "net" : {
            "http" : {
                "uri" : {
                    "args" : {
                        "foo${rnd.nextInt()}" : "bar${time:millisSinceEpoch()}",
                        "key1" : "${date:formatNowIso8601()}",
                        "${e}" : "${pi}"
                    }
                }
            }
        }
    }
};

Load
    .config(uriQueryConfig)
    .run();
```

will produce the HTTP requests with URIs like:
`/20190306.104255.627/kticoxcknpuy?key1=2019-03-06T10:42:56,768&2.718281828459045=3.141592653589793&foo1130828259=bar1551868976768`

**Note**:
> Don't use both synchronous and asynchronous expressions for the query args simultaneously. All configured query args
> are collected into the single expression input.
