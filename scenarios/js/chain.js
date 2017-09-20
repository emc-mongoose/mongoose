var itemOutputFile = "items_passed_through_create_read_delete_chain.csv"

var createConfig = {
    "test" : {
        "step" : {
            "limit" : {
                "time" : "5m"
            }
        }
    }
};

var readConfig = {
    "load" : {
        "type" : "read"
    }
};

var deleteConfig = {
    "item" : {
        "output" : {
            "file" : itemOutputFile
        }
    },
    "load" : {
        "type" : "delete"
    }
};

command
    .value("rm -f " + itemOutputFile)
    .run();

chain
    .config(createConfig)
    .config(readConfig)
    .config(deleteConfig)
    .run();
