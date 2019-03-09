package com.emc.mongoose.base.item

import com.github.akurilov.commons.io.Input
import com.github.akurilov.commons.system.SizeInBytes
import org.junit._
import java.io.EOFException
import java.util
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder

import com.emc.mongoose.base.config.ConstantValueInputImpl
import com.emc.mongoose.base.item.op.data.{DataOperation, DataOperationsBuilderImpl}
import com.emc.mongoose.base.item.io.{DelayedTransferConvertBuffer, NewDataItemInput}
import com.emc.mongoose.base.item.naming.ItemNameInputImpl
import com.emc.mongoose.base.item.op.{OpType, OperationsBuilder}
import com.emc.mongoose.base.storage.Credential
import it.unimi.dsi.fastutil.longs.Long2LongFunction
import org.junit.Assert.fail

final class DelayedTransferBufferPerfTest {

	private val BATCH_SIZE = 0x1000
	private val BUFF_CAPACITY = 1000000
	private val TIMEOUT = 100

	private var itemInput = null : Input[DataItem]
	private var ioTaskBuilder = null : OperationsBuilder[DataItem, DataOperation[DataItem]]
	private var buff = null : TransferConvertBuffer[DataItem, DataOperation[DataItem]]

	@Before @throws[Exception]
	def setUp(): Unit = {
		itemInput = new NewDataItemInput[DataItem](
			ItemType getItemFactory (ItemType DATA),
			new ItemNameInputImpl(
				new Long2LongFunction {
					override def get(v: Long): Long = v + 1
				},
				12, null, Character.MAX_RADIX
			),
			new SizeInBytes(0)
		)
		ioTaskBuilder = new DataOperationsBuilderImpl[DataItem, DataOperation[DataItem]](0)
		ioTaskBuilder opType(OpType NOOP)
		ioTaskBuilder outputPathInput new ConstantValueInputImpl[String]("/default")
		ioTaskBuilder credentialInput new ConstantValueInputImpl[Credential](Credential getInstance("uid1", "secret1"))
		buff = new DelayedTransferConvertBuffer[DataItem, DataOperation[DataItem]](
			BUFF_CAPACITY, 0, TimeUnit SECONDS
		)
	}

	@After @throws[Exception]
	def tearDown(): Unit = {
		ioTaskBuilder close()
		buff close()
	}

	@Test @throws[Exception]
	def testIoRate(): Unit = {

		val OP_COUNTER: LongAdder = new LongAdder

		val producerThread: Thread = new Thread(() => {
			val dataItemsBuff: util.List[DataItem] = new util.ArrayList[DataItem](BATCH_SIZE)
			val ioTaskBuff: util.List[DataOperation[DataItem]] = new util.ArrayList[DataOperation[DataItem]](BATCH_SIZE)
			try {
				while(true) {
					if(BATCH_SIZE != itemInput.get(dataItemsBuff, BATCH_SIZE)) {
						fail()
					}
					ioTaskBuilder.buildOps(dataItemsBuff, ioTaskBuff)
					var n: Int = 0
					while(n < BATCH_SIZE) {
						n += buff.put(ioTaskBuff, n, BATCH_SIZE)
					}
					dataItemsBuff.clear()
					ioTaskBuff.clear()
				}
			} catch {
				case e: Exception => e printStackTrace System.err
			}
		})

		val consumerThread: Thread = new Thread(() => {
			val dataItemsBuff: util.List[DataItem] = new util.ArrayList[DataItem](BATCH_SIZE)
			var n: Int = 0
			try {
				while({
					n = buff.get(dataItemsBuff, BATCH_SIZE)
					n >= 0
				}) {
					OP_COUNTER.add(n)
					dataItemsBuff.clear()
				}
			} catch {
				case ignored: EOFException =>
				case e: Exception => e printStackTrace System.err
			}
		})

		producerThread.start()
		consumerThread.start()
		TimeUnit.SECONDS.sleep(TIMEOUT)
		producerThread.interrupt()
		consumerThread.interrupt()
		System.out.println("I/O rate: " + OP_COUNTER.sum / TIMEOUT)
	}
}
