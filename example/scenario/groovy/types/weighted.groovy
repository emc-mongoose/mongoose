final sharedConcurrency = 100
final itemDataSize = "10KB"
final itemsFile = "weighted_load_example.csv"
final itemOutputPath = "/weighted_load_example"

// declare the cleanup shell command
final Precondition1 = Command
    .value("rm -f $itemsFile".toString())

// prepare (create) the 10000 items on a storage before the test
final Precondition2 = PreconditionLoad
    .config(
        [
            item : [
                data : [
                    size : itemDataSize
                ],
                output : [
                    file : itemsFile,
                    path : itemOutputPath
                ]
            ],
            load : [
                limit : [
                    concurrency : sharedConcurrency
                ]
            ],
            test : [
                step : [
                    limit : [
                        count : 10000
                    ]
                ]
            ]
        ]
    )

// declare the weighted load step instance (20% create operations, 80% read operations)
final WeightedLoad1 = WeightedLoad
    .config(
        [
            item : [
                data : [
                    size : itemDataSize
                ],
                output : [
                    path : itemOutputPath
                ]
            ],
            load : [
                generator : [
                    weight : 20
                ]
            ],
            test : [
                step : [
                    limit : [
                        time : 100
                    ]
                ]
            ]
        ]
    )
    .config(
        [
            item : [
                input : [
                    file : itemsFile
                ]
            ],
            load : [
                generator : [
                    recycle : [
                        enabled : true
                    ],
                    weight : 80
                ],
                type : "read"
            ]
        ]
    )

// go
Precondition1.run()
Precondition2.run()
WeightedLoad1.run()
