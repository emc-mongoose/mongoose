noOutputColorConfig = {
    "output": {
        "color": False
    }
}

command1 = command\
    .config(noOutputColorConfig)\
    .value("echo \"Hello world!\"")

command2 = command\
    .value("ps alx | grep java")

parallel\
    .step(command1)\
    .step(command2)\
    .run()
