itemInputFile =
itemOutputFile =

config = {
	"storage" : {
		"driver" : {
			"type" : "swift"
		},
		"namespace" : "AUTH_test",
		"net": {
			"node" : {
				"port" : 8080
			}
		},
		"auth" : {
			"uid" : "test:tester",
			"secret" : "ievahsaelaiz"
		},
		"load": {
             "op": {
                  "limit": {
                       "count": 10
                  }
             }
        }
	}
}

read_config = {
        "item": {
            "input": {
                "file": itemInputFile
            },
            "data": {
            	"verify" : true,
            	"ranges" : {
                    "random" : 10
                }
            }
        }
      }
create_config = {
        "item": {
            "output": {
                "file": itemOutputFile
            }
        }
    }

PreconditionLoad
	.config(config)
    .config(create_config)
    .run();

ReadLoad
	.config(config)
	.config(read_config)
	.run()
