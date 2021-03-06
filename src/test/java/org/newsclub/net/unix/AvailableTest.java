/**
 * junixsocket
 *
 * The author licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.newsclub.net.unix;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.Test;


public class AvailableTest extends SocketTestBase {

    public AvailableTest () throws IOException {
        super();
    }

    private final int bytesSent = 23;
    private final int timeToSleep = 100;


    static void receiveBytes ( final Socket sock, final int expected ) throws IOException {
        @SuppressWarnings ( "resource" )
        InputStream in = sock.getInputStream();

        int toExpect = expected;

        char firstChar = 'A';

        int available = in.available();
        if ( available == 0 && expected != 0 ) {
            // this may happen, and it's ok.
            int r = in.read();
            assertEquals("Available returned 0, so we tried to read the first byte (which should be 65=='A')", 'A', r);

            // as we have already read one byte, we now expect one byte less
            toExpect--;

            available = in.available();

            firstChar = 'B';
        }
        assertEquals(toExpect, available);
        byte[] buf = new byte[expected];
        int numRead = in.read(buf);
        assertEquals(toExpect, numRead);

        for ( int i = 0; i < numRead; i++ ) {
            assertEquals(firstChar + i, buf[ i ] & 0xFF);
        }

        assertEquals(0, in.available());
    }


    void sendBytes ( final Socket sock ) throws IOException {
        @SuppressWarnings ( "resource" )
        OutputStream out = sock.getOutputStream();
        byte[] buf = new byte[this.bytesSent];
        for ( int i = 0; i < this.bytesSent; i++ ) {
            buf[ i ] = (byte) ( i + 'A' );
        }
        out.write(buf);
        out.flush();
    }


    /**
     * Tests if {@link InputStream#available()} works as expected. The server
     * sends 23 bytes. The client waits for 100ms. After that, the client should
     * be able to read exactly 23 bytes without blocking. Then, we try the
     * opposite direction.
     * 
     * @throws Exception
     */
    @Test ( timeout = 2000 )
    public void testAvailableAtClient () throws Exception {

        ServerThread serverThread = new ServerThread() {

            @Override
            protected void handleConnection ( final AFUNIXSocket sock ) throws IOException {
                sendBytes(sock);
                sleepFor(AvailableTest.this.timeToSleep);
                receiveBytes(sock, AvailableTest.this.bytesSent);
            }
        };

        try ( AFUNIXSocket sock = connectToServer() ) {
            sleepFor(this.timeToSleep);
            receiveBytes(sock, this.bytesSent);
            sendBytes(sock);
        }

        serverThread.checkException();
    }


    /**
     * Tests if {@link InputStream#available()} works as expected. The client
     * sends 23 bytes. The server waits for 100ms. After that, the server should
     * be able to read exactly 23 bytes without blocking. Then, we try the
     * opposite direction.
     * 
     * @throws Exception
     */
    @Test ( timeout = 2000 )
    public void testAvailableAtServer () throws Exception {

        ServerThread serverThread = new ServerThread() {

            @Override
            protected void handleConnection ( final AFUNIXSocket sock ) throws IOException {
                sleepFor(AvailableTest.this.timeToSleep);
                receiveBytes(sock, AvailableTest.this.bytesSent);
                sendBytes(sock);
            }
        };

        try ( AFUNIXSocket sock = connectToServer() ) {
            sendBytes(sock);
            sleepFor(this.timeToSleep);

            receiveBytes(sock, this.bytesSent);
        }

        serverThread.checkException();
    }
}
