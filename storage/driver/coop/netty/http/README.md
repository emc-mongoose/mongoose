# HTTP Storage Drivers

## 1. Configuration Reference

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| storage-net-http-headers                       | Map          | { "Connection" : "keep-alive", "User-Agent" : "mongoose/4.0.2" } | Custom HTTP headers section. An user may place here a key-value pair which will be used as HTTP header. The headers will be appended to every HTTP request issued.
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

### 2.1. Parameterizing

Scenario example, note the parameterized header name:
```javascript
var varHttpHeadersConfig = {
    "storage" : {
        "net" : {
            "http" : {
                "headers" : {
                    "x-amz-meta-$d[1-30]" : "%D{yyyy-MM-dd'T'HH:mm:ssZ}[1970/01/01-2016/01/01]"
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

TODO
