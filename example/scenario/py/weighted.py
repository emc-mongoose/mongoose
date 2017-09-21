sharedConcurrency = 100
itemDataSize = "10KB"
itemsFile = "weighted_load_example.csv"
itemOutputPath = "/weighted_load_example"

precondition1 = command\
    .value("rm -f " + itemsFile)

precondition2 = load\
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
            "output": {
                "metrics": {
                    "average": {
                        "persist": False
                    },
                    "summary": {
                        "persist": False
                    },
                    "trace": {
                        "persist": False
                    }
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
    )

weightedLoadStep = weighted\
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
    )\
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
                        "enabled": True
                    },
                    "weight": 80
                },
                "type": "read"
            }
        }
    )

precondition1.run()
precondition2.run()
weightedLoadStep.run()
