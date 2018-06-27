var sharedConcurrency = 100;
var itemDataSize = "10KB";
var itemsFile = "weighted_load_example.csv";
var itemOutputPath = "/weighted_load_example";

// declare the cleanup shell command
new java.lang.ProcessBuilder("sh", "-c", "rm -f " + itemsFile)
    .start()
    .waitFor();

// prepare (create) the 10000 items on a storage before the test
Load
    .config(
        {
            "item": {
                "data": {
                    "size": itemDataSize
                },
                "output": {
                    "file": itemsFile,
                    "path": itemOutputPath
                }
            },
            "load": {
                "limit": {

                },
                "step": {
                    "limit": {
                        "concurrency": sharedConcurrency,
                        "count": 10000
                    }
                }
            }
        }
    )
    .run();

// declare the weighted load step instance (20% create operations, 80% read operations)
WeightedLoad
    .append(
        {
            "item": {
                "data": {
                    "size": itemDataSize
                },
                "output": {
                    "path": itemOutputPath
                }
            },
            "load": {
                "generator": {
                    "weight": 20
                },
                "step": {
                    "limit": {
                        "time": 100
                    }
                }
            }
        }
    )
    .append(
        {
            "item": {
                "input": {
                    "file": itemsFile
                }
            },
            "load": {
                "generator": {
                    "recycle": {
                        "enabled": true
                    },
                    "weight": 80
                },
                "type": "read"
            }
        }
    )
    .run();
