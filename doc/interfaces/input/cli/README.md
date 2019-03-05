GNU style command line arguments are used:

```bash
java mongoose-<VERSION>.jar \
    --read \
    --item-input-file=items.csv \
    --load-step-limit-concurrency=10 \
    --storage-auth-uid=user1 \
    --storage-auth-secret=ChangeIt \
    --storage-net-node-addrs=10.20.30.40,10.20.30.41
```

The command-line options are directly mapped to the configuration items. For example the configuration option
`item-data-size` corresponds to the CLI argument `--item-data-size`.
