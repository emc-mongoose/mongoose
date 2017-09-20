final sharedConcurrency = 100
final itemDataSize = "10KB"
final itemsFile = "weighted_load_example.csv"
final itemOutputPath = "/weighted_load_example"

final precondition1 = command
    .value("rm -f " + itemsFile)

final precondition2 = load
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
            output : [
                metrics : [
                    average : [
                        persist : false
                    ],
                    summary : [
                        persist : false
                    ],
                    trace : [
                        persist : false
                    ]
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

final weightedLoadStep = weighted
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

precondition1.run()
precondition2.run()
weightedLoadStep.run()
