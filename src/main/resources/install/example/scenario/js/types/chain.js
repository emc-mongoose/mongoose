var itemOutputFile = "items_passed_through_create_read_delete_chain.csv"

// limit the whole chain step execution time by 5 minutes
// (chain step takes the limits configuration parameter values from the 1st configuration element)
var createConfig = {
    "test": {
        "step": {
            "limit": {
                "time": "5m"
            }
        }
    }
};

var readConfig = {
    "load": {
        "type": "read"
    }
};

// persist the items info into the output file after the last operation
var deleteConfig = {
    "item": {
        "output": {
            "file": itemOutputFile
        }
    },
    "load": {
        "type": "delete"
    }
};

// clean up before running the chain load step
var cmd = new java.lang.ProcessBuilder("sh", "-c", "rm -f " + itemOutputFile).start();
cmd.waitFor();

ChainLoad
    .config(createConfig)
    .config(readConfig)
    .config(deleteConfig)
    .run();
