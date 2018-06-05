var itemDataSizes = ["10KB", "1MB", "100MB"];
var limitCount = 10000;
var limitTime = "1s";
var limitConcurrency = [1,10,100];
var fileName="10K_items.csv"

function printToCL(cmd) {
    var cmdStdOut = new java.io.BufferedReader(
            new java.io.InputStreamReader(cmd.getInputStream())
    );
    cmd.waitFor();
    while(null != (nextLine = cmdStdOut.readLine())) {
            print(nextLine);
    }
    cmdStdOut.close();
}

var cmd = new java.lang.ProcessBuilder()
    .command("sh", "-c", "rm $ITEM_INPUT_FILE; echo $ITEM_INPUT_FILE")
    .start();
printToCL(cmd);



var load10K_config =
{
    "item": {
        "output": {
              "file": ITEM_INPUT_FILE,
        }
    },
    "load": {
        "step": {
              "limit": {
                    "count": limitCount,
              }
        }
    }
};

PreconditionLoad
    .config(load10K_config)
    .run();


var read_config =
{
    "item": {
        "input": {
            "file": ITEM_INPUT_FILE
        }
    },
    "load": {
        "generator": {
            "recycle": {
                "enabled": true
            }
        },
        "type": "read"
    }
};


function createConfig(size, concLimit) {
    return {
                          "item": {
                              "data": {
                                  "size": size
                              }
                          },
                          "load": {
                                  "step": {
                                        "limit": {
                                              "concurrency": concLimit,
                                              "time": limitTime
                                        }
                                  }
                          }
                      };
}


var readStep = Load.config(read_config);


for(var size = 0; size < itemDataSizes.length; ++size) {

    print("\n\n\nSIZE : " + itemDataSizes[size]);

    for(var conc = 0; conc < limitConcurrency.length; ++conc) {

        print("\nConcurrency LIMIT : " + limitConcurrency[conc]);

    var createStep = Load.config(createConfig(itemDataSizes[size], limitConcurrency[conc]));

    readStep.start();
    createStep.start();

    createStep.await();
    readStep.stop();

    createStep.close();
    readStep.close();

    }
}