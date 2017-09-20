var noOutputColorConfig = {
    "output" : {
        "color" : false
    }
};

var command1 = command
    .config(noOutputColorConfig)
    .value("echo \"Hello world!\"");

var command2 = command
    .value("ps alx | grep java");

parallel
    .step(command1)
    .step(command2)
    .run();
