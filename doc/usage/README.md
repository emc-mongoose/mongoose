# Contents

1. Input<br/>
1.1. [Defaults](defaults/README.md)<br/>
1.2. [Configuration](../input/configuration/README.md)<br/>
1.2.1. [CLI](../input/configuration/README.md#11-cli)<br/>
1.2.2. [Reference Table](../input/configuration/README.md#12-reference-table)<br/>
1.2.3. [Parameterizing](../input/configuration/README.md#2-parameterization)<br/>
1.2.4. Storage Drivers<br/>
1.2.4.1. [Netty Based](../../storage/driver/coop/netty/README.md)<br/>
1.2.4.1.1. [HTTP Storage Drivers](../../storage/driver/coop/netty/http/README.md)<br/>
1.2.4.1.1.1. [Atmos](../../storage/driver/coop/netty/http/atmos/README.md)<br/>
1.2.4.1.1.2. [S3](../../storage/driver/coop/netty/http/s3/README.md)<br/>
1.2.4.1.1.3. [Swift](../../storage/driver/coop/netty/http/swift/README.md)<br/>
1.2.4.2. [Filesystem](../../storage/driver/coop/nio/fs/README.md)<br/>
1.3. [Scenarios](../input/scenarios/README.md)<br/>
2. [Output](../output/README.md)<br/>
2.1. [General](../output#1-general)<br/>
2.2. [Metrics](../output#2-metrics)<br/>
3. Load Generation<br/>
3.1. Items<br/>
3.1.1. [Types](item/types)<br/>
3.1.1.1. [Data](item/types#1-data)<br/>
3.1.1.1.1. [Size](item/types#11-size)<br/>
3.1.1.1.1.1. [Fixed](item/types#111-fixed)<br/>
3.1.1.1.1.2. [Random](item/types#112-random)<br/>
3.1.1.1.2. [Payload](item/types#12-payload)<br/>
3.1.1.1.2.1. [Random Using A Seed](item/types#121-random-using-a-seed)<br/>
3.1.1.1.2.2. [Custom Using An External File](item/types#122-custom-using-an-external-file)<br/>
3.1.1.2. [Path](item/types#2-path)<br/>
3.1.1.3. [Token](item/types#3-token)<br/>
3.1.2. [Input](item/input)<br/>
3.1.2.1. [File](item/input#1-file)<br/>
3.1.2.2. [Item Path Listing Input](item/input#2-item-path-listing-input)<br/>
3.1.2.3. [New Items Input](item/input#3-new-items-input)<br/>
3.1.2.3.1. [Naming](item/input#31-naming)<br/>
3.1.2.3.1.1. [Types](item/input#311-types)<br/>
3.1.2.3.1.1.1. [Random](item/input#3111-random)<br/>
3.1.2.3.1.1.2. [Ascending](item/input#3112-ascending)<br/>
3.1.2.3.1.1.1. [Descending](item/input#3113-descending)<br/>
3.1.2.3.1.2. [Prefix](item/input#312-prefix)<br/>
3.1.2.3.1.3. [Radix](item/input#313-radix)<br/>
3.1.2.3.1.4. [Offset](item/input#314-offset)<br/>
3.1.2.3.1.5. [Length](item/input#315-length)<br/>
3.1.3. [Output](item/output)<br/>
3.1.3.1. [File](item/output#1-file)<br/>
3.1.3.1. [Path](item/output#2-path)<br/>
3.2. Load Operations<br/>
3.2.1. Types<br/>
3.2.1.1. Create<br/>
3.2.1.1.1. Basic<br/>
3.2.1.1.2. Copy Mode<br/>
3.2.1.2. Read<br/>
3.2.1.2.1. Basic<br/>
3.2.1.2.2. Content Verification<br/>
3.2.1.3. Update<br/>
3.2.1.4. Delete<br/>
3.2.1.5. Noop<br/>
3.2.2. Byte Ranges Operations<br/>
3.2.2.1. Read<br/>
3.2.2.1.1. Random Ranges<br/>
3.2.2.1.2. Fixed Ranges<br/>
3.2.2.2. Update<br/>
3.2.2.2.1. Random Ranges<br/>
3.2.2.2.2. Fixed Ranges<br/>
3.2.2.2.2.1. Append<br/>
3.2.3. Composite Operations<br/>
3.2.3.1. Storage-Side Concatenation<br/>
3.2.3.1.1. S3 Multipart Upload<br/>
3.2.3.1.2. Swift Dynamic Large Objects<br/>
3.2.4. Recycle Mode<br/>
3.3. Load Steps<br/>
3.3.1. Identification<br/>
3.3.2. Limits<br/>
3.3.2.1. Operations Count Limit<br/>
3.3.2.2. Time Limit<br/>
3.3.2.3. Transfer Size Limit<br/>
3.3.2.4. End Of Input<br/>
3.3.3. Types<br/>
3.3.3.1. Linear<br/>
3.3.3.2. [Pipeline](../../load/step/pipeline/README.md)<br/>
3.3.3.3. [Weighted Load](../../load/step/weighted/README.md)<br/>
4. [Load Scaling](scaling)<br/>
4.1. [Rate](scaling/README.md#1-rate)<br/>
4.2. [Concurrency](scaling/README.md#2-concurrency)<br/>
4.3. [Distributed Mode](scaling/README.md#3-distributed-mode)<br/>
