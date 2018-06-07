var sharedConfig = {
    "storage": {
        "net": {
            "http": {
                "namespace": "ns1"
            }
        }
    }
};

PreconditionLoad
    .config(sharedConfig)
    .config(
        {
            "item": {
                "output": {
                    "path": ITEM_SRC_PATH
                }
            },
            "load": {
                "step": {
                    "limit": {
                        "count": 100000,
                        "size": "1GB",
                        "time": 30
                    }
                }
            }
        }
    )
    .run();

Load
    .config(
        {
            "item": {
                "input": {
                    "path": ITEM_SRC_PATH
                },
                "output": {
                    "path": ITEM_DST_PATH
                }
            }
        }
    )
    .run();
