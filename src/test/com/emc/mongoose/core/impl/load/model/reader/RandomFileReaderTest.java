package com.emc.mongoose.core.impl.load.model.reader;
//mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.model.reader.io.LineReader;
import com.emc.mongoose.core.impl.load.model.reader.util.Randomizer;
//
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
import org.junit.runner.RunWith;
//
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
//
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
* RandomFileReader Tester. 
* 
* @author zhavzo
* @since <pre>May 15, 2015</pre> 
* @version 1.0 
*/ 

@RunWith(MockitoJUnitRunner.class)
public final class RandomFileReaderTest {

    @Mock
    private LineReader reader;

    @Mock
    private Randomizer random;

    @Before
    public final void before()
    throws Exception {
    }

    @After
    public final void after()
       throws Exception {
    }

    @Test
    public void shouldReadSingleLine() throws Exception {
        final RandomFileReader randomReader = new RandomFileReader(reader, 0, Integer.MAX_VALUE, random);

        when(reader.readLine())
            .thenReturn("line #1")
            .thenReturn(null);
        assertEquals(
            "line #1",
            randomReader.readLine()
        );
    }

    @Test
    public void shouldReturnNullWhenReachedEndOfFile() throws Exception {
        final RandomFileReader randomReader = new RandomFileReader(reader, 0, Integer.MAX_VALUE, random);

        when(reader.readLine())
            .thenReturn("line #1")
            .thenReturn(null);

        randomReader.readLine();

        assertNull(
            randomReader.readLine()
        );
    }

    @Test
    public void shouldReadLinesInNaturalOrder() throws Exception {
        final RandomFileReader randomReader = new RandomFileReader(reader, 0, Integer.MAX_VALUE, random);

        when(reader.readLine())
            .thenReturn("line #01")
            .thenReturn("line #02")
            .thenReturn("line #03")
            .thenReturn("line #04")
            .thenReturn("line #05")
            .thenReturn(null);

        assertArrayEquals(
            new String[]{
                "line #01",
                "line #02",
                "line #03",
                "line #04",
                "line #05",
                null
            },
            new String[]{
                randomReader.readLine(),
                randomReader.readLine(),
                randomReader.readLine(),
                randomReader.readLine(),
                randomReader.readLine(),
                randomReader.readLine()
            }
        );
    }

    @Test
    public void shouldReadLinesInOrderDefinedByRandomizer() throws Exception {
        int batch = 5;

        final RandomFileReader randomReader = new RandomFileReader(
            reader,
            batch,
            Integer.MAX_VALUE,
            random
        );

        when(reader.readLine())
            .thenReturn("line #00")
            .thenReturn("line #01")
            .thenReturn("line #02")
            .thenReturn("line #03")
            .thenReturn("line #04")
            .thenReturn(null);

        when(random.nextInt(anyInt()))
            .thenReturn(4)
            .thenReturn(2)
            .thenReturn(1)
            .thenReturn(1)
            .thenReturn(0);

        assertArrayEquals(
            new String[]{
                "line #04",
                "line #02",
                "line #01",
                "line #03",
                "line #00",
                null
            },
            new String[]{
                randomReader.readLine(),
                randomReader.readLine(),
                randomReader.readLine(),
                randomReader.readLine(),
                randomReader.readLine(),
                randomReader.readLine()
            }
        );
    }

    @Test
    public void shouldReadLinesFromFileByBatch() throws Exception {
        int batch = 3;

        final RandomFileReader randomReader = new RandomFileReader(
            reader,
            batch,
            Integer.MAX_VALUE,
            random
        );

        when(reader.readLine())
            .thenReturn("line #00")
            .thenReturn("line #01")
            .thenReturn("line #02");

        when(random.nextInt(anyInt()))
            .thenReturn(0);


        randomReader.readLine();

        verify(reader, atLeastOnce()).readLine();
    }

    @Test
    public void shouldKeepLinesBufferFull() throws Exception {
        int batch = 3;

        final RandomFileReader randomReader = new RandomFileReader(
            reader,
            batch,
            Integer.MAX_VALUE,
            random
        );

        when(reader.readLine())
            .thenReturn("line #00")
            .thenReturn("line #01")
            .thenReturn("line #02")
            .thenReturn("line #03");

        when(random.nextInt(anyInt()))
            .thenReturn(0);

        randomReader.readLine();
        randomReader.readLine();

        verify(reader, atLeastOnce()).readLine();
    }

    @Test
    public void shouldReadAllLinesWhenBatchIsBiggerThanLineNumber() throws Exception {
        int batchSize = 10;
        final RandomFileReader randomReader = new RandomFileReader(reader, batchSize, Integer.MAX_VALUE, random);

        when(reader.readLine())
            .thenReturn("line #01")
            .thenReturn("line #02")
            .thenReturn("line #03")
            .thenReturn("line #04")
            .thenReturn("line #05")
            .thenReturn(null);

        when(random.nextInt(anyInt())).thenReturn(0);

        assertArrayEquals(
            new String[]{
              "line #01",
              "line #02",
              "line #03",
              "line #04",
              "line #05",
              null
            },
            new String[] {
              randomReader.readLine(),
              randomReader.readLine(),
              randomReader.readLine(),
              randomReader.readLine(),
              randomReader.readLine(),
              randomReader.readLine()
            }
        );
    }

    @Test
    public void shouldCloseReader() throws Exception {
        final RandomFileReader randomReader = new RandomFileReader(reader, 0, Integer.MAX_VALUE, random);

        randomReader.close();

        verify(reader).close();
    }

} 
