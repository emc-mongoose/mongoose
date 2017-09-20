var sharedConcurrency = 100;
var itemDataSize = "10KB";
var itemsFile = "weighted_load_example.csv";
var itemOutputPath = "/weighted_load_example";

var precondition1 = command
    .value("rm -f " + itemsFile);

var precondition2 = load
    .config(
        {
            "item" : {
                "data" : {
                    "size" : itemDataSize
                },
                "output" : {
                    "file" : itemsFile,
                    "path" : itemOutputPath
                }
            },
            "load" : {
                "limit" : {
                    "concurrency" : sharedConcurrency
                }
            },
            "output" : {
                "metrics" : {
                    "average" : {
                        "persist" : false
                    },
                    "summary" : {
                        "persist" : false
                    },
                    "trace" : {
                        "persist" : false
                    }
                }
            },
            "test" : {
                "step" : {
                    "limit" : {
                        "count" : 10000
                    }
                }
            }
        }
    );

var weightedLoadStep = weighted
    .config(
        {
            "item" : {
                "data" : {
                    "size" : itemDataSize
                },
                "output" : {
                    "path" : itemOutputPath
                }
            },
            "load" : {
                "generator" : {
                    "weight" : 20
                }
            },
            "test" : {
                "step" : {
                    "limit" : {
                        "time" : 100
                    }
                }
            }
        }
    )
    .config(
        {
            "item" : {
                "input" : {
                    "file" : itemsFile
                }
            },
            "load" : {
                "generator" : {
                    "recycle" : {
                        "enabled" : true
                    },
                    "weight" : 80
                },
                "type" : "read"
            }
        }
    );

precondition1.run();
precondition2.run();
weightedLoadStep.run();
