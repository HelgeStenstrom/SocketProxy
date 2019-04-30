package com.ericsson.erahege;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SimpleProxyServerTest {


    @DataProvider(name = "escapeStrings")
    public static Object[][] escapeStrings() {
        return new Object[][] {
                {"abc", "abc"},
                {"a c", "a c"},
                {"a\rc", "a\\rc"},
                {"a\nc", "a\\nc"},
                {"a\tc", "a\\tc"},
                {"a\0c", "a\\0c"},
        };
    }

    @Test(dataProvider = "escapeStrings")
    public void testEscape(String unescaped, String escaped) {
        assertEquals(SimpleProxyServer.escape(unescaped), escaped);
    }
}