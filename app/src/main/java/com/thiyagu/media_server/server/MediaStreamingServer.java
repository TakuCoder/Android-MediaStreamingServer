package com.thiyagu.media_server.server;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class MediaStreamingServer extends NanoHTTPD {


    public static final String TAG = MediaStreamingServer.class.getSimpleName();



    private String mediaFilePath;

    private int port = 0;

    public MediaStreamingServer(String filepath, int port) {
        super(port);
        mediaFilePath = filepath;
        this.port = port;

    }




    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        Map<String, String> parms = session.getParms();
        Method method = session.getMethod();
        String uri = session.getUri();
        Map<String, String> files = new HashMap<>();

        if (Method.POST.equals(method) || Method.PUT.equals(method)) {
            try {
                session.parseBody(files);
            }
            catch (IOException e) {
                return getResponse("Internal Error IO Exception: " + e.getMessage());
            }
            catch (ResponseException e) {
                return  Response.newFixedLengthResponse(e.getStatus(), MIME_PLAINTEXT, e.getMessage());
            }
        }

        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        File f = new File(mediaFilePath);
        return serveFile(mediaFilePath, headers, f);
    }

    private Response serveFile(String uri, Map<String, String> header, File file) {
        Response res;
        String mime = getMimeTypeForFile(uri);
        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() +
                    file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Change return code and add Content-Range header when skipping is requested
            long fileLen = file.length();
            if (range != null && startFrom >= 0) {
                if (startFrom >= fileLen) {
                    res = createResponse(Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    final long dataLen = newLen;
                    FileInputStream fis = new FileInputStream(file) {
                        @Override
                        public int available() throws IOException {
                            return (int) dataLen;
                        }
                    };
                    fis.skip(startFrom);

                    res = createResponse(Status.PARTIAL_CONTENT, mime, fis);
                    res.addHeader("Content-Length", "" + dataLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" +
                            endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {
                if (etag.equals(header.get("if-none-match")))
                    res = createResponse(Status.NOT_MODIFIED, mime, "");
                else {
                    res = createResponse(Status.OK, mime, new FileInputStream(file));
                    res.addHeader("Content-Length", "" + fileLen);
                    res.addHeader("ETag", etag);
                }
            }
        } catch (IOException ioe) {
            res = getResponse("Forbidden: Reading file failed");
        }

        return (res == null) ? getResponse("Error 404: File not found") : res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Status status, String mimeType, InputStream message) throws IOException {
        Response res = Response.newFixedLengthResponse(status, mimeType, message,message.available());
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    // Announce that the file server accepts partial content requests
    private Response createResponse(Status status, String mimeType, String message) {
        Response res = Response.newFixedLengthResponse(status, mimeType, message);
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    private Response getResponse(String message) {
        return createResponse(Status.OK, "text/plain", message);
    }






public String getServerUrl(String ipAddress)
{
    String ip = ipAddress;
    if(port ==80)
    {
        return "http://$ipAddress/"  ;

    }
    if (ipAddress.indexOf(":") >= 0)
    {
        // IPv6
        int pos = ipAddress.indexOf("%");
        // java insists in adding %wlan and %p2p0 to everything
        if (pos > 0) {
            ip = ipAddress.substring(0, pos);
        }
        return "http://"+ip+":"+port+"/";
    }
    return "http://"+ip+":"+port+"/";

}

}