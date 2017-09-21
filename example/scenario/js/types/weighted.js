var sharedConcurrency = 100;
var itemDataSize = "10KB";
var itemsFile = "weighted_load_example.csv";
var itemOutputPath = "/weighted_load_example";

// declare the cleanup shell command
var precondition1 = Command
    .value("rm -f " + itemsFile);

// prepare (create) the 10000 items on a storage before the test
var precondition2 = PreconditionLoad
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
                    "concurrency": sharedConcurrency
                }
            },
            "test": {
                "step": {
                    "limit": {
                        "count": 10000
                    }
                }
            }
        }
    );

// declare the weighted load step instance (20% create operations, 80% read operations)
var weightedLoad1 = WeightedLoad
    .config(
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
                }
            },
            "test": {
                "step": {
                    "limit": {
                        "time": 100
                    }
                }
            }
        }
    )
    .config(
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
    );

// go
precondition1.run();
precondition2.run();
weightedLoad1.run();
