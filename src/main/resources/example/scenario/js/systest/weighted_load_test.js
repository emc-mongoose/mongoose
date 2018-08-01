var cmd_1 = new java.lang.ProcessBuilder()
    .command("/bin/sh", "-c", "rm -f weighted-load.csv")
    .inheritIO()
    .start();
cmd_1.waitFor();

PreconditionLoad
    .config({
      "load" : {
        "step" : {
          "limit" : {
            "count" : 10000,
            "time" : 10,
            "size" : "1GB"
          }
        }
      },
      "item" : {
        "output" : {
          "file" : "weighted-load.csv"
        }
      }
    })
    .run();

WeightedLoad
    .config({
      "load" : {
        "step" : {
          "limit" : {
            "time" : 50
          }
        }
      }
    })
    .append({
      "load" : {
        "op" : {
          "weight" : 20
        }
      },
      "item" : {
        "output" : {
          "path" : "" + ITEM_OUTPUT_PATH + ""
        }
      }
    })
    .append({
      "load" : {
        "op" : {
          "recycle" : {
            "enabled" : true
          },
          "type" : "read",
          "weight" : 80
        }
      },
      "item" : {
        "input" : {
          "file" : "weighted-load.csv"
        }
      }
    })
    .run();
