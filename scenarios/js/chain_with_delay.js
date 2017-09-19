const config1 = {
    "item" : {
        "output" : {
            "delay" : "1m",
            "path" : "/default"
        }
    },
    "storage" : {
        "net" : {
            "node" : {
                "addrs" : ZONE1_ADDRS
            }
        }
    }
};

const config2 = {
    "load" : {
        "type" : "read"
    },
    "storage" : {
        "net" : {
            "node" : {
                "addrs" : ZONE2_ADDRS
            }
        }
    }
};

chain
    .config([config1, config2])
    .run();
