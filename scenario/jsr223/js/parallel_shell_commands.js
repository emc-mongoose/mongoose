var noOutputColorConfig = {
    "output" : {
        "color" : false
    }
};

var commands = [
    command
        .config(noOutputColorConfig)
        .value("echo \"Hello world!\""),
    command
        .value("ps alx | grep java"),
];

var scenario = parallel.steps(commands);
