PipelineLoad
    .append({
      "item" : {
        "output" : {
          "delay" : "1m"
        }
      },
      "storage" : {
        "net" : {
          "node" : {
            "addrs" : ZONE1_ADDRS
          }
        }
      }
    })
    .append({
      "load" : {
        "type" : "read"
      },
      "storage" : {
        "net" : {
          "node" : {
            "addrs" : ZONE2_ADDRS
          }
        }
      }
    })
    .run();
