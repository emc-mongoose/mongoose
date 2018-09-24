# Contents

1. Input<br/>
1.1. [Defaults](defaults)<br/>
1.2. [Configuration](../input/configuration)<br/>
1.2.1. [CLI](../input/configuration#11-cli)<br/>
1.2.2. [Reference Table](../input/configuration#12-reference-table)<br/>
1.2.3. [Parameterizing](../input/configuration#2-parameterization)<br/>
1.2.4. Storage Drivers<br/>
1.2.4.1. [Netty Based](../../storage/driver/coop/netty)<br/>
1.2.4.1.1. [HTTP Storage Drivers](../../storage/driver/coop/netty/http)<br/>
1.2.4.1.1.1. [Atmos](../../storage/driver/coop/netty/http/atmos)<br/>
1.2.4.1.1.2. [S3](../../storage/driver/coop/netty/http/s3)<br/>
1.2.4.1.1.3. [Swift](../../storage/driver/coop/netty/http/swift)<br/>
1.2.4.2. [Filesystem](../../storage/driver/coop/nio/fs)<br/>
1.3. [Scenarios](../input/scenarios)<br/>
2. [Output](../output)<br/>
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
3.2.1. [Types](load/operations/types)<br/>
3.2.1.1. [Create](load/operations/types#1-create)<br/>
3.2.1.1.1. [Basic](load/operations/types#11-basic)<br/>
3.2.1.1.2. [Copy Mode](load/operations/types#12-copy-mode)<br/>
3.2.1.2. [Read](load/operations/types#2-read)<br/>
3.2.1.2.1. [Basic](load/operations/types#21-basic)<br/>
3.2.1.2.2. [Content Verification](load/operations/types#22-content-verification)<br/>
3.2.1.3. [Update](load/operations/types#3-update)<br/>
3.2.1.4. [Delete](load/operations/types#4-delete)<br/>
3.2.1.5. [Noop](load/operations/types#5-noop)<br/>
3.2.2. [Byte Ranges Operations](load/operations/byte_ranges)<br/>
3.2.2.1. [Random Ranges](load/operations/byte_ranges#41-random-ranges)<br/>
3.2.2.2. [Fixed Ranges](load/operations/byte_ranges#42-fixed-ranges)<br/>
3.2.2.2.1. [Append](load/operations/byte_ranges#421-append)<br/>
3.2.3. [Composite Operations](load/operations/composite)<br/>
3.2.3.1. [Storage-Side Concatenation](load/operations/composite#1-storage-side-concatenation)<br/>
3.2.3.1.1. [S3 Multipart Upload](load/operations/composite#131-s3-multipart-upload)<br/>
3.2.3.1.2. [Swift Dynamic Large Objects](load/operations/composite#132-swift-dynamic-large-objects)<br/>
3.2.4. [Recycling](load/operations/recycling)<br/>
3.3. [Load Steps](load/steps)<br/>
3.3.1. [Identification](load/steps#1-identification)<br/>
3.3.2. [Limits](load/steps#2-limits)<br/>
3.3.2.1. [Operations Count](load/steps#21-operations-count)<br/>
3.3.2.2. [Time](load/steps#22-time)<br/>
3.3.2.3. [Transfer Size](load/steps#23-transfer-size)<br/>
3.3.2.4. [End Of Input](load/steps#24-end-of-input)<br/>
3.3.3. Types<br/>
3.3.3.1. [Linear](../../load/step/linear)<br/>
3.3.3.2. [Pipeline](../../load/step/pipeline)<br/>
3.3.3.3. [Weighted Load](../../load/step/weighted)<br/>
4. [Load Scaling](scaling)<br/>
4.1. [Rate](scaling#1-rate)<br/>
4.2. [Concurrency](scaling#2-concurrency)<br/>
4.3. [Distributed Mode](scaling3-distributed-mode)<br/>
