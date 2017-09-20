final noOutputColorConfig = [
    output : [
        color : false
    ]
]

final command1 = command
    .config(noOutputColorConfig)
    .value("echo \"Hello world!\"")

final command2 = command.value("ps alx | grep java")

parallel
    .step(command1)
    .step(command2)
    .run()
