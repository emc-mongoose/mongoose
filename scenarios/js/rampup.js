// increase the concurrency level by the factor of 10
var concurrency_limit_factor = 10;

var item_data_sizes = [
    "10KB", "1MB", "100MB", "10GB"
];

// initial concurrency limit value
var concurrency_limit = 1;

// typically OS open files limit is 1024 so don't try to use the concurrency level higher than that
while(concurrency_limit < 1024) {

    for(var i = 0; i < item_data_sizes.length; i ++) {

        item_data_size = item_data_sizes[i];

        command
            .value(
                "echo \"Run the load steps using the concurrency limit of "
                    + concurrency_limit + " and items data size " + item_data_size + "\""
            )
            .run()

        var config_concurrency_limit = {
            "load": {
                "limit": {
                    "concurrency": concurrency_limit
                }
            }
        };

        var config_item_data_size = {
            "item": {
                "data": {
                    "size": item_data_size
                }
            }
        }

        var iter_id = "concurrency" + concurrency_limit + "_size" + item_data_size;
        var iter_items_file_0 = "items_" + iter_id + "_0.csv";
        var iter_items_file_1 = "items_" + iter_id + "_1.csv";

        create_load
            .config(config_concurrency_limit)
            .config(config_item_data_size)
            .config(
                {
                    "item": {
                        "output": {
                            "file" : iter_items_file_0
                        }
                    },
                    "test": {
                        "step": {
                            "id": "create0_" + iter_id,
                            "limit": {
                                "count": 1000000,
                                "time": 100
                            }
                        }
                    }
                }
            )
            .run();

        read_load
            .config(config_concurrency_limit)
            .config(
                {
                    "item": {
                        "input": {
                            "file": iter_items_file_0
                        }
                    },
                    "test": {
                         "step": {
                             "id": "read1_" + iter_id
                         }
                    }
                }
            )
            .run();

        update_random_range_load
            .config(config_concurrency_limit)
            .config(
                {
                    "item": {
                        "input": {
                            "file": iter_items_file_0
                        },
                        "output": {
                            "file": iter_items_file_1
                        }
                    },
                    "test": {
                        "step": {
                            "id": "update2_" + iter_id
                        }
                    }
                }
            )
            .run();

        read_and_verify_random_range_load
            .config(config_concurrency_limit)
            .config(
                {
                    "item": {
                        "input": {
                            "file": iter_items_file_1
                        }
                    },
                    "test": {
                        "step": {
                            "id": "read3_" + iter_id
                        }
                    }
                }
            )
            .run();

        delete_load
            .config(config_concurrency_limit)
            .config(
                {
                    "item": {
                        "input": {
                            "file": iter_items_file_0
                        }
                    },
                    "test": {
                        "step": {
                            "id": "delete4_" + iter_id
                        }
                    }
                }
            )
    }

    concurrency_limit *= concurrency_limit_factor;
    // cast concurrency_limit value to int
    concurrency_limit = ~~config_concurrency_limit;
}
