// increase the concurrency level by the factor of 10
var limit_concurrency_factor = 10;

var item_data_sizes = [
    "10KB", "1MB", "100MB", "10GB"
];

var limit_count = 1000000;
var limit_time = 100;
// initial concurrency limit value
var limit_concurrency = 1;

function config_limit_concurrency(concurrency) {
    return {
        "load": {
            "limit": {
                "concurrency": concurrency
            }
        }
    }
};

function config_item_data_size(size) {
    return {
        "item": {
            "data": {
                "size": size
            }
        }
    }
};

function config_create(iter_id, size, item_output_file, limit_count, limit_time) {
    return {
        "item": {
            "data" : {
                "size" : size
            },
            "output": {
                "file" : item_output_file
                "path" : "/default"
            }
        },
        "test": {
            "step": {
                "id": "create0_" + iter_id,
                "limit": {
                    "count": limit_count,
                    "time": limit_time
                }
            }
        }
    }
};

function config_read(iter_id, item_input_file) {
    return {
        "item": {
            "input": {
                "file": item_input_file
            }
        },
        "test": {
             "step": {
                 "id": "read1_" + iter_id
             }
        }
    }
};

function config_update(iter_id, item_input_file, item_output_file) {
    return {
        "item": {
            "input": {
                "file": item_input_file
            },
            "output": {
                "file": item_output_file
            }
        },
        "test": {
            "step": {
                "id": "update2_" + iter_id
            }
        }
    }
};

function config_read_partial(iter_id, item_input_file) {
    return {
        "item": {
            "input": {
                "file": item_input_file
            }
        },
        "test": {
            "step": {
                "id": "read3_" + iter_id
            }
        }
    }
};

function config_delete(iter_id, item_input_file) {
    return {
        "item": {
            "input": {
                "file": item_input_file
            }
        },
        "test": {
            "step": {
                "id": "delete4_" + iter_id
            }
        }
    }
};

// typically OS open files limit is 1024 so won't try to use the concurrency level higher than that
while(limit_concurrency < 1024) {

    for(var i = 0; i < item_data_sizes.length; i ++) {

        item_data_size = item_data_sizes[i];
        print(
            "Run the load steps using the concurrency limit of " + limit_concurrency
                + " and items data size " + item_data_size
        );
        var next_config_limit_concurrency = config_limit_concurrency(limit_concurrency);
        var iter_id = "concurrency" + limit_concurrency + "_size" + item_data_size;
        var iter_items_file_0 = "items_" + iter_id + "_0.csv";
        var iter_items_file_1 = "items_" + iter_id + "_1.csv";

        create_load
            .config(next_config_limit_concurrency)
            .config(config_item_data_size)
            .config(
                config_create(
                    iter_id, item_data_size, iter_items_file_0, limit_count, limit_time
                )
            )
            .run();

        read_load
            .config(next_config_limit_concurrency)
            .config(config_read(iter_id, iter_items_file_0))
            .run();

        update_random_range_load
            .config(next_config_limit_concurrency)
            .config(config_update(iter_id, iter_items_file_0, iter_items_file_1))
            .run();

        read_and_verify_random_range_load
            .config(next_config_limit_concurrency)
            .config(config_read_partial(iter_id, iter_items_file_1))
            .run();

        delete_load
            .config(next_config_limit_concurrency)
            .config(config_delete(iter_id, iter_items_file_0))
            .run();
    }

    limit_concurrency *= limit_concurrency_factor;
    // cast limit_concurrency value to int
    limit_concurrency = ~~limit_concurrency;
}
