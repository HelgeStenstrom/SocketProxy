package com.ericsson.erahege;
// This example is from _Java Examples in a Nutshell_. (http://www.oreilly.com)
// Copyright (c) 1997 by David Flanagan
// This example is provided WITHOUT ANY WARRANTY either expressed or implied.
// You may study, use, modify, and distribute it for non-commercial purposes.
// For any commercial use, see http://www.davidflanagan.com/javaexamples

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static java.util.Arrays.copyOfRange;

/**
 * This class implements a simple single-threaded proxy server.
 **/
public class SimpleProxyServer {
    /** The main method parses arguments and passes them to runServer */
    public static void main(String[] args) {
        try {
            // Check the number of arguments
            if (args.length != 3)
                throw new IllegalArgumentException("Wrong number of arguments.");

            // Get the command-line arguments: the host and port we are proxy for
            // and the local port that we listen for connections on
            String host = args[0];
            int remoteport = Integer.parseInt(args[1]);
            int localport = Integer.parseInt(args[2]);
            // Print a start-up message
            System.out.println("Starting proxy for " + host + ":" + remoteport +
                    " on port " + localport);
            // And start running the server
            runServer(host, remoteport, localport);   // never returns
        }
        catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java SimpleProxyServer " +
                    "<host> <remoteport> <localport>");
        }
    }

    /**
     * This method runs a single-threaded proxy server for
     * host:remotePort on the specified local port.  It never returns.
     **/
    public static void runServer(String host, int remotePort, int localport)
            throws IOException {
        // Create a ServerSocket to listen for connections with
        ServerSocket ss = new ServerSocket(localport);

        // Create buffers for client-to-server and server-to-client communication.
        // We make one final so it can be used in an anonymous class below.
        // Note the assumptions about the volume of traffic in each direction...
        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];

        // This is a server that never returns, so enter an infinite loop.
        while(true) {
            // Variables to hold the sockets to the client and to the server.
            Socket client = null;
            Socket server = null;
            try {
                // Wait for a connection on the local port
                client = ss.accept();

                // Get client streams.  Make them final so they can
                // be used in the anonymous thread below.
                final InputStream fromClient = client.getInputStream();
                final OutputStream toClient= client.getOutputStream();

                // Make a connection to the real server
                // If we cannot connect to the server, send an error to the
                // client, disconnect, then continue waiting for another connection.
                try { server = new Socket(host, remotePort); }
                catch (IOException e) {
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(toClient));
                    out.println("Proxy server cannot connect to " + host + ":" +
                            remotePort + ":\n" + e);
                    out.flush();
                    client.close();
                    continue;
                }

                // Get server streams.
                final InputStream fromServer = server.getInputStream();
                final OutputStream toServer = server.getOutputStream();

                // Make a thread to read the client's requests and pass them to the
                // server.  We have to use a separate thread because requests and
                // responses may be asynchronous.
                Thread t = new Thread() {
                    public void run() {
                        int bytes_read;
                        try {
                            while((bytes_read = fromClient.read(request)) != -1) {
                                toServer.write(request, 0, bytes_read);
                                toServer.flush();
                                final String unescaped = new String(copyOfRange(request, 0, bytes_read));
                                System.out.println("==> " + escape(unescaped));
                            }
                        }
                        catch (IOException e) {
                            // ignore exception
                        }

                        // the client closed the connection to us, so  close our
                        // connection to the server.  This will also cause the
                        // server-to-client loop in the main thread exit.
                        try {toServer.close();} catch (IOException ignored) {
                            // ignore the exception
                        }
                    }
                };

                // Start the client-to-server request thread running
                t.start();

                // Meanwhile, in the main thread, read the server's responses
                // and pass them back to the client.  This will be done in
                // parallel with the client-to-server request thread above.
                int bytesRead;
                try {
                    while((bytesRead = fromServer.read(reply)) != -1) {
                        toClient.write(reply, 0, bytesRead);
                        toClient.flush();

                        char[] chars = new char[bytesRead];
                        for (int i = 0; i < bytesRead; i++) {
                            chars[i] = (char) reply[i];
                        }
                        String escaped = escape(new String(chars));
                        System.out.printf("<== '%s' (%d bytes)%n", escaped, bytesRead);
                    }
                }
                catch(IOException ignored) {
                    // ignore exception
                }

                // The server closed its connection to us, so close our
                // connection to our client.  This will make the other thread exit.
                toClient.close();
            }
            catch (IOException e) { System.err.println(e); }
            // Close the sockets no matter what happens each time through the loop.
            finally {
                try {
                    if (server != null) server.close();
                    if (client != null) client.close();
                }
                catch(IOException ignored) {
                    // ignore exception
                }
            }
        }
    }

    /**
     * @param unescaped string potentially containing unprintable control characters.
     * @return string where some control character have been replaced with printable escape sequences like \0, \r. \n and \t.
     */
    public static String escape(String unescaped) {
        String escaped = "";
        for (char c : unescaped.toCharArray()) {
            switch (c) {
                case '\0':
                    escaped += "\\0";
                    break;
                case '\n':
                    escaped += "\\n";
                    break;
                case '\r':
                    escaped += "\\r";
                    break;
                case '\t':
                    escaped += "\\t";
                    break;
                default:
                    if (c >= ' ' && c < 127) {
                        escaped += c;
                    } else {
                        escaped += String.format("\\0x%02X", (int) c);
                    }
            }
        }
        return escaped;
    }
}
