package com.emc.mongoose.base.item

import com.github.akurilov.commons.system.SizeInBytes
import org.junit.{Assert, Before, Test}
import java.io.EOFException
import java.nio.file.{Files, Paths}
import java.util
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder

import com.emc.mongoose.base.item.io.{CsvFileItemInput, CsvFileItemOutput, NewDataItemInput}
import com.emc.mongoose.base.item.naming.ItemNameInputImpl
import it.unimi.dsi.fastutil.longs.Long2LongFunction

final class CsvFileItemInputPerfTest {

	private val BATCH_SIZE = 0x1000
	private val FILE_NAME = "items.csv"

	@Before @throws[Exception]
	def setUp(): Unit = {
		try Files delete(Paths get FILE_NAME)
		catch {
			case ignored: Exception =>
		}
	}

	@Test @throws[Exception]
	def testInputRate(): Unit = {
		val count = 100000000
		val itemFactory = ItemType.getItemFactory[DataItem, ItemFactory[DataItem]](ItemType.DATA)
		val itemBuff = new util.ArrayList[DataItem](BATCH_SIZE)
		val itemNameInput = new ItemNameInputImpl(
			new Long2LongFunction {
				override def get(v: Long): Long = v + 1
			},
			12, null, Character.MAX_RADIX)
		val newItemsInput = new NewDataItemInput[DataItem](itemFactory, itemNameInput, new SizeInBytes("0-1MB,2"))
		try {
			val newItemsOutput = new CsvFileItemOutput[DataItem](
				Paths.get(FILE_NAME), itemFactory
			)
			try {
				var n = 0
				var m = 0
				while(n < count) {
					m = newItemsInput get(itemBuff, BATCH_SIZE)
					var i = 0
					while(i < m) {
						i += newItemsOutput.put(itemBuff, i, m)
					}
					n += m
					itemBuff clear()
				}
			} finally {
				if(newItemsOutput != null) {
					newItemsOutput close()
				}
			}
		}
		finally {
			if(newItemsInput != null) {
				newItemsInput close()
			}
		}
		System.out.println("Items input file is ready, starting the input")
		val inputCounter = new LongAdder
		var t = System nanoTime
		val fileItemInput = new CsvFileItemInput[DataItem](Paths get FILE_NAME, itemFactory)
		try {
			var n = 0
			while({
				n = fileItemInput.get(itemBuff, BATCH_SIZE)
				n > 0
			}) {
				inputCounter add n
				itemBuff clear()
			}
		} catch {
			case ignored: EOFException =>
		} finally {
			if(fileItemInput != null) {
				fileItemInput close()
			}
		}
		t = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime - t)
		Assert.assertEquals(count, inputCounter.sum, BATCH_SIZE)
		System.out.println("CSV file input rate: " + count / t + " items per second")
	}
}
