package test.com.emc.mongoose.core.impl.load.model.reader;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
//mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.reader.io.LineReader;
import com.emc.mongoose.core.impl.load.model.reader.RandomFileReader;
//
import com.emc.mongoose.core.impl.load.model.reader.util.Randomizer;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
//
import java.io.IOException;
import java.util.Random;
import java.util.RandomAccess;

import static org.junit.Assert.assertEquals;

/** 
* RandomFileReader Tester. 
* 
* @author zhavzo
* @since <pre>May 15, 2015</pre> 
* @version 1.0 
*/ 
public final class RandomFileReaderTest {

   private final IMocksControl control = createControl();

   @Before
   public final void before()
   throws Exception {
   }

   @After
   public final void after()
   throws Exception {
   }

   @Test
   public final void testSynchronousReadEntireFile() {
      final int size = 100;
      final int batch = 0;
      try {
         final LineReader reader = control.createMock(LineReader.class);
         final Randomizer random = control.createMock(Randomizer.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, Integer.MAX_VALUE, random);

         for(int i = 0; i < size; i++) {
            expect(reader.readLine()).andReturn(Integer.toString(i));
         }
         expect(reader.readLine()).andReturn(null);    // EOF by null

         control.replay();

         int countReadLines = 0;
         String actualLine;
         String expectedLine;

         do {
            actualLine = randomReader.readLine();
            expectedLine = Integer.toString(countReadLines);
            if(countReadLines < size) {
               assertEquals(expectedLine, actualLine);
            }
            countReadLines ++;
         } while (actualLine != null);

         control.verify();

      }catch (final IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }catch (Exception e){
         e.printStackTrace();
      }
   }

   @Test
   public final void testRandomReadEntireFile() {
      final int size = 100;
      final int batch = 100;
      try {
         final LineReader reader = control.createMock(LineReader.class);
         final Randomizer random = control.createMock(Randomizer.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, Integer.MAX_VALUE, random);

         prepare(size, batch, reader, random);
         expect(reader.readLine()).andReturn(null);    // EOF by null

         control.replay();

         String actualLine;
         String expectedLine;
         int i = size - 1;

         do {
            actualLine = randomReader.readLine();
            expectedLine = Integer.toString(i);
            if(i >= 0) {
               assertEquals(expectedLine, actualLine);
            }
            i--;
         } while (actualLine != null);

         control.verify();

      }catch (final IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }catch (Exception e){
         e.printStackTrace();
      }
   }

   @Test
   public final void testRandomBatchedReadEntireFile() {
      final int size = 100;
      final int batch = 15;
      try {
         final LineReader reader = control.createMock(LineReader.class);
         final Randomizer random = control.createMock(Randomizer.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, Integer.MAX_VALUE, random);

         prepare(size, batch, reader, random);
         expect(reader.readLine()).andReturn("");    // EOF by empty string

         control.replay();

         int countReadLines = 0;
         String actualLine;
         String expectedLine;

         do {
            actualLine = randomReader.readLine();
            if (countReadLines < size) {
               if(countReadLines <= size - batch){
                  expectedLine = Integer.toString(batch + countReadLines - 1);
               } else {
                  expectedLine = Integer.toString(size - countReadLines - 1);
               }
               assertEquals(expectedLine, actualLine);
            }
            countReadLines ++;
         } while (actualLine != null);

         control.verify();

      }catch (final IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }catch (Exception e){
         e.printStackTrace();
      }
   }

   @Test
   public final void testBatchedMaxCount() {
      final int maxCount = 10;
      final int batch = 3;
      try {
         final LineReader reader = control.createMock(LineReader.class);
         final Randomizer random = control.createMock(Randomizer.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, maxCount, random);

         prepare(maxCount, batch, reader, random);
         // No EOF; stop by maxCount

         control.replay();

         int countReadLines = 0;
         String actualLine;
         String expectedLine;

         do {
            actualLine = randomReader.readLine();
            if (countReadLines < maxCount) {
               if(countReadLines <= maxCount - batch){
                  expectedLine = Integer.toString(batch + countReadLines - 1);
               } else {
                  expectedLine = Integer.toString(maxCount - countReadLines - 1);
               }
               assertEquals(expectedLine, actualLine);
            }
            countReadLines ++;
         } while (actualLine != null);

         //Assert max count where actualMaxCount = countReadLines - null line
         assertEquals(maxCount, countReadLines - 1);

         control.verify();

      }catch (final IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }catch (Exception e){
         e.printStackTrace();
      }
   }

   @Test
   public final void testMaxCount() throws IOException {
      final int maxCount = 10;
      final int batch = 10000;
      try {
         final LineReader reader = control.createMock(LineReader.class);
         final Randomizer random = control.createMock(Randomizer.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, maxCount, random);

         prepare(maxCount, batch, reader, random);
         // No EOF; stop by maxCount

         control.replay();

         int countReadLines = 0;
         String actualLine;
         String expectedLine;
         int i = maxCount - 1;

         do {
            actualLine = randomReader.readLine();
            expectedLine = Integer.toString(i);
            if(i >= 0) {
               assertEquals(expectedLine, actualLine);
            }
            i --;
            countReadLines ++;
         } while (actualLine != null);

         //Assert max count where actualMaxCount = countReadLines - null line
         assertEquals(maxCount, countReadLines - 1);

         control.verify();

      }catch (final IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }catch (Exception e){
         e.printStackTrace();
      }
   }

   private void prepare(final int size, final int batch, final LineReader reader, final Randomizer random)
   throws IOException {
      for(int i = 0; i < size; i++) {
         expect(reader.readLine()).andReturn(Integer.toString(i));
         if(batch >= size) {
            final int r = size - i;
            expect(random.nextInt(r)).andReturn(r - 1);
         } else {
            final int r = i <= size - batch ? batch : size - i;
            expect(random.nextInt(r)).andReturn(r - 1);
         }
      }
   }

} 
