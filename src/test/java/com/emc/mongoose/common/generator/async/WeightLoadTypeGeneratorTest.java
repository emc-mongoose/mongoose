package com.emc.mongoose.common.generator.async;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.generator.ValueGenerator;
import org.junit.Assert;
import org.junit.Test;
/**
 Created by kurila on 28.03.16.
 */
public class WeightLoadTypeGeneratorTest {

	private ValueGenerator<LoadType> loadTypeGenerator = new WeightLoadTypeGenerator(
		"mixed{write=80;read=20}"
	);

	@Test
	public void testGet()
	throws Exception {
		int
			nWrites = 0, nReads = 0, nSeqWrites = 0, nSeqReads = 0,
			maxWriteSeqSize = Integer.MIN_VALUE, maxReadSeqSize = Integer.MIN_VALUE;
		boolean
			prevWrite = false, prevRead = false;
		LoadType loadType;
		for(int i = 0; i < 1000000; i ++) {
			loadType = loadTypeGenerator.get();
			if(LoadType.WRITE.equals(loadType)) {
				nWrites ++;
				if(prevWrite) {
					nSeqWrites ++;
				} else {
					if(nSeqWrites > maxWriteSeqSize) {
						maxWriteSeqSize = nSeqWrites;
					}
					nSeqWrites = 0;
				}
				prevRead = false;
				prevWrite = true;
			} else if(LoadType.READ.equals(loadType)) {
				nReads ++;
				if(prevRead) {
					nSeqReads ++;
				} else {
					if(nSeqReads > maxReadSeqSize) {
						maxReadSeqSize = nSeqReads;
					}
					nSeqReads = 0;
				}
				prevWrite = false;
				prevRead = true;
			}
		}
		System.out.println(maxWriteSeqSize);
		System.out.println(maxReadSeqSize);
		Assert.assertEquals(800000, nWrites, 1000);
		Assert.assertEquals(200000, nReads, 1000);
	}
}
