var fileName = "CircularReadLimitByTime.csv";

var cmd = new java.lang.ProcessBuilder()
    .command("sh", "-c", "rm -f " + fileName)
    .inheritIO()
    .start();
cmd.waitFor();

PreconditionLoad
    .config({
      "load" : {
        "step" : {
          "limit" : {
            "count" : 1
          }
        }
      },
      "item" : {
        "output" : {
          "file" : fileName
        }
      }
    })
    .run();

ReadLoad
    .config({
      "load" : {
        "type" : "read",
        "generator" : {
          "recycle" : {
            "enabled" : true
          }
        },
        "step" : {
          "limit" : {
            "time" : "1m"
          }
        }
      },
      "item" : {
        "input" : {
          "file" : fileName
        }
      }
    })
    .run();
