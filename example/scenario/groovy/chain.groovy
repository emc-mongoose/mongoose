final itemOutputFile = "items_passed_through_create_read_delete_chain.csv"

final createConfig = [
    test : [
        step : [
            limit : [
                time : "5m"
            ]
        ]
    ]
]

final readConfig = [
    load : [
        type : "read"
    ]
]

final deleteConfig = [
    item : [
        output : [
            file : itemOutputFile
        ]
    ],
    load : [
        type : "delete"
    ]
]

command
    .value("rm -f " + itemOutputFile)
    .run()

chain
    .config(createConfig)
    .config(readConfig)
    .config(deleteConfig)
    .run()
