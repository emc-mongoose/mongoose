sharedConcurrency = 100
itemDataSize = "10KB"
itemsFile = "weighted_load_example.csv"
itemOutputPath = "/weighted_load_example"

# declare the cleanup shell command
precondition1 = Command \
    .value("rm -f " + itemsFile)

# prepare (create) the 10000 items on a storage before the test
precondition2 = PreconditionLoad \
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

# declare the weighted load step instance (20% create operations, 80% read operations)
weightedLoad1 = WeightedLoad \
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
    ) \
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
                        "enabled": True
                    },
                    "weight": 80
                },
                "type": "read"
            }
        }
    )

# go
precondition1.run()
precondition2.run()
weightedLoad1.run()
