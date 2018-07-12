var fileName = "ReadVerificationAfterCircularUpdateTest";

var cmd_1 = new java.lang.ProcessBuilder()
    .command("/bin/sh", "-c", "rm -f " + fileName + "0.csv " + fileName + "1.csv")
    .inheritIO()
    .start();
cmd_1.waitFor();

var step_1 = PreconditionLoad
    .config({
      "load" : {
        "step" : {
          "limit" : {
            "count" : 1000
          }
        }
      },
      "item" : {
        "output" : {
          "file" : fileName + "0.csv"
        }
      }
    })
    .run();

PreconditionLoad
    .config({
      "load" : {
        "type" : "update",
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
        "data" : {
          "ranges" : {
            "random" : 10
          }
        },
        "output" : {
          "file" : fileName + "1.csv"
        },
        "input" : {
          "file" : fileName + "0.csv"
        }
      }
    })
    .run();

var cmd_2 = new java.lang.ProcessBuilder()
    .command("/bin/sh", "-c", "sleep 5")
    .inheritIO()
    .start();
cmd_2.waitFor();

ReadLoad
    .config({
      "load" : {
        "type" : "read"
      },
      "item" : {
        "data" : {
          "verify" : true
        },
        "input" : {
          "file" : fileName + "1.csv"
        }
      }
    })
    .run();
