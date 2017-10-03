var noOutputColorConfig = {
    "output": {
        "color": false
    }
};

var command1 = Command
    .config(noOutputColorConfig)
    .value("echo Hello world!");

var command2 = Command
    .value("ps alx | grep java");

Parallel
    .step(command1)
    .step(command2)
    .run();
