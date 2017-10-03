final noOutputColorConfig = [
    output : [
        color : false
    ]
]

final command1 = Command
    .config(noOutputColorConfig)
    .value("echo Hello world!")

final command2 = Command
    .value("ps alx | grep java")

Parallel
    .step(command1)
    .step(command2)
    .run()
