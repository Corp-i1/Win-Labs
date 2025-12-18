package com.winlabs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GreeterTest {
    
    @Test
    void testGreet() {
        Greeter greeter = new Greeter();
        String result = greeter.greet("World");
        assertEquals("Hello, World!", result);
    }
    
    @Test
    void testGreetWithDifferentName() {
        Greeter greeter = new Greeter();
        String result = greeter.greet("Developer");
        assertEquals("Hello, Developer!", result);
    }
}
