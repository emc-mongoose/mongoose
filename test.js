swift_item_list = "swift_item_list.csv"

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
		}
	},
	"load": {
         "op": {
              "limit": {
                   "count": 1
              }
         }
    }
}

read_config = {
        "item": {
            "input": {
                "file": swift_item_list
            }
        }
      }
create_config = {
        "item": {
            "output": {
                "file": swift_item_list
            }
        },
        "load" : {
        	"step" : {
        		"limit" : {
        			"size" : 10
        		}
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