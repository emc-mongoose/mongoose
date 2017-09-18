const noOutputColorConfig = {
    "output" : {
        "color" : false
    }
};

const commands = [
    command
        .config(noOutputColorConfig)
        .value("echo \"Hello world!\""),
    command
        .value("ps alx | grep java"),
];

parallel
    .steps(commands)
    .run();
