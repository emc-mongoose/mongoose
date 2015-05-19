package test.com.emc.mongoose.core.impl.load.model.reader;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;
////mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.reader.RandomFileReader;
//
import org.easymock.IMocksControl;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/** 
* RandomFileReader Tester. 
* 
* @author zhavzo
* @since <pre>May 15, 2015</pre> 
* @version 1.0 
*/ 
public class RandomFileReaderTest {

   private final IMocksControl control = createControl();


   @Before
   public void before() throws Exception {
   }

   @After
   public void after() throws Exception {
   }
   
   /**
    *
    * Method: fillUp()
    *
    */

   @Test
   public void testFillUp() throws Exception {
      //TODO: Test goes here...
   }

   @Test
   public void testSynchronousReadEntireFile(){
      final int size = 100;
      final int batch = 0;
      try {
         final BufferedReader reader = control.createMock(BufferedReader.class);
         final Random random = control.createMock(Random.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, Integer.MAX_VALUE, random);

         for(int i = 0; i < size; i++) {
            expect(reader.readLine()).andReturn(Integer.toString(i));
         }
         expect(reader.readLine()).andReturn(null);    // EOF by null

         control.replay();

         String line;
         int i = 0;

         do {
            line = randomReader.readLine();
            if(i < size) {
               assertEquals(Integer.toString(i), line);
            }
            i ++;
         } while (line != null);

         control.verify();

      }catch (IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }
   }

   @Test
   public void testRandomReadEntireFile() throws IOException {
      final int size = 100;
      final int batch = 100;
      try {
         final BufferedReader reader = control.createMock(BufferedReader.class);
         final Random random = control.createMock(Random.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, Integer.MAX_VALUE, random);

         prepare(size, batch, reader, random);
         expect(reader.readLine()).andReturn(null);    // EOF by null

         control.replay();

         String line;
         int i = size - 1;

         do {
            line = randomReader.readLine();

            if(i >= 0) {
               assertEquals(Integer.toString(i), line);
            }
            i--;
         } while (line != null);

         control.verify();


      }catch (IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }
   }

   @Test
   public void testMaxCount() throws IOException {
      final int maxCount = 50;
      final int batch = 1000;
      try {
         final BufferedReader reader = control.createMock(BufferedReader.class);
         final Random random = control.createMock(Random.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, maxCount, random);

         prepare(maxCount, batch, reader, random);
         // No EOF; stop by maxCount

         control.replay();

         String line;
         int i = maxCount - 1;

         do {
            line = reader.readLine();

            if(i >= 0) {
               assertEquals(Integer.toString(i), line);
               i--;
            }
         } while (line != null);

         control.verify();

      }catch (IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }
   }

   @Test
   public void testRandomBatchedReadEntireFile() throws IOException {
      final int size = 100;
      final int batch = 15;
      try {
         final BufferedReader reader = control.createMock(BufferedReader.class);
         final Random random = control.createMock(Random.class);
         final RandomFileReader randomReader = new RandomFileReader(reader, batch, Integer.MAX_VALUE, random);

         prepare(size, batch, reader, random);
         expect(reader.readLine()).andReturn("");    // EOF by empty str

         control.replay();

         String line;
         int i = 0;

         do {
            line = reader.readLine();

            if (i < size) {
               if (i <= size - batch) {
                  assertEquals(Integer.toString(batch - 1 + i), line);
               } else {
                  assertEquals(Integer.toString(size - i - 1), line);
               }
            }
            i++;
         } while (line != null);

         control.verify();
      }catch (IOException e) {
         System.err.print(String.format("Failed to read line: %s", e.getMessage()));
      }
   }


   private void prepare(int size, int batch, final BufferedReader reader, final Random random) throws IOException {
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
