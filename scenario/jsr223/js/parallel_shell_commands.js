parallel
    .steps(
        [
            command
                .config(
                    {
                        "output" : {
                            "color" : false
                        }
                    }
                )
                .value("echo \"Hello world!\""),
            command
                .value("ps alx | grep java"),
        ]
    )
