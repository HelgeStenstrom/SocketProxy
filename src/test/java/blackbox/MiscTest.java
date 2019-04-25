package blackbox;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class MiscTest {

    @Test
    void byteArrayConversion() {
        // From https://www.mkyong.com/java/how-do-convert-byte-array-to-string-in-java/
        String example = "This is an example\r\n>;";
        byte[] bytes = example.getBytes();

        String s = new String(bytes);

        assertEquals(s, example);
    }
}
