

var cmd = new java.lang.ProcessBuilder()
    .command("/bin/sh", "-c", "rm -f " + FILE_NAME)
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
          "file" : FILE_NAME
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
          "file" : FILE_NAME
        }
      }
    })
    .run();
