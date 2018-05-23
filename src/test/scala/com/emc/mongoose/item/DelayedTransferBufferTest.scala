package com.emc.mongoose.item

import com.emc.mongoose.item.io.IoType
import com.emc.mongoose.item.io.task.IoTaskBuilder
import com.emc.mongoose.item.io.task.data.BasicDataIoTaskBuilder
import com.emc.mongoose.item.io.task.data.DataIoTask
import com.emc.mongoose.supply.ConstantStringSupplier
import com.github.akurilov.commons.io.Input
import com.github.akurilov.commons.system.SizeInBytes
import org.junit._
import java.io.EOFException
import java.util
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder

import org.junit.Assert.fail

final class DelayedTransferBufferTest {

	private val BATCH_SIZE = 0x1000
	private val BUFF_CAPACITY = 1000000
	private val TIMEOUT = 100

	private var itemInput = null : Input[DataItem]
	private var ioTaskBuilder = null : IoTaskBuilder[DataItem, DataIoTask[DataItem]]
	private var buff = null : TransferConvertBuffer[DataItem, DataIoTask[DataItem]]

	@Before @throws[Exception]
	def setUp(): Unit = {
		itemInput = new NewDataItemInput[DataItem](
			ItemType getItemFactory ItemType.DATA,
			new ItemNameSupplier(ItemNamingType.ASC, null, 13, Character.MAX_RADIX, 0),
			new SizeInBytes(0)
		)
		ioTaskBuilder = new BasicDataIoTaskBuilder[DataItem, DataIoTask[DataItem]](0)
		ioTaskBuilder.setIoType(IoType.NOOP)
		ioTaskBuilder.setOutputPathSupplier(new ConstantStringSupplier("/default"))
		ioTaskBuilder.setUidSupplier(new ConstantStringSupplier("uid1"))
		ioTaskBuilder.setSecretSupplier(new ConstantStringSupplier("secret1"))
		buff = new DelayedTransferConvertBuffer[DataItem, DataIoTask[DataItem]](
			BUFF_CAPACITY, TimeUnit.SECONDS, 0
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
			val ioTaskBuff: util.List[DataIoTask[DataItem]] = new util.ArrayList[DataIoTask[DataItem]](BATCH_SIZE)
			try {
				while(true) {
					if(BATCH_SIZE != itemInput.get(dataItemsBuff, BATCH_SIZE)) {
						fail()
					}
					ioTaskBuilder.getInstances(dataItemsBuff, ioTaskBuff)
					var n: Int = 0
					while(n < BATCH_SIZE) {
						n += buff.put(ioTaskBuff, n, BATCH_SIZE)
					}
					dataItemsBuff.clear()
					ioTaskBuff.clear()
				}
			} catch {
				case e: Exception => e.printStackTrace(System.err)
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
				case e: Exception => e.printStackTrace(System.err)
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
