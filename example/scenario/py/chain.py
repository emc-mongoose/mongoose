itemOutputFile = "items_passed_through_create_read_delete_chain.csv"

createConfig = {
    "test": {
        "step": {
            "limit": {
                "time": "5m"
            }
        }
    }
}

readConfig = {
    "load": {
        "type": "read"
    }
}

deleteConfig = {
    "item": {
        "output": {
            "file": itemOutputFile
        }
    },
    "load": {
        "type": "delete"
    }
}

command\
    .value("rm -f " + itemOutputFile)\
    .run()

chain\
    .config(createConfig)\
    .config(readConfig)\
    .config(deleteConfig)\
    .run()
