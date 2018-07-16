Load
    .config({
      "load" : {
        "step" : {
          "limit" : {
            "count" : 1000000,
            "time" : "1m",
            "rate" : 1000,
            "concurrency" : 0
          }
        }
      }
    })
    .run();
