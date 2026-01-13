package dev.qilletni.lib.lastfm.music.auth;

import dev.qilletni.api.lib.persistence.PackageConfig;
import dev.qilletni.lib.lastfm.music.api.LastFmAPI;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class LastFmAuthorizer {
    
    private static final String AUTH_REQUEST_URL = "http://www.last.fm/api/auth/?api_key=%s";
    private static final String SESSION_KEY_NAME = "sessionKey";

    private final PackageConfig packageConfig;
    private final String apiKey;
    private final String apiSecret;
    
    private LastFmAPI lastFmAPI;
    
    public LastFmAuthorizer(PackageConfig packageConfig, String apiKey, String apiSecret) {
        this.packageConfig = packageConfig;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    public CompletableFuture<LastFmAPI> authorizeLastFm() {
        var completableFuture = new CompletableFuture<LastFmAPI>();

        packageConfig.get(SESSION_KEY_NAME).ifPresentOrElse(
                sessionKey -> completableFuture.complete(lastFmAPI = new LastFmAPI(apiKey, apiSecret, sessionKey)),
                () -> requestSessionToken().thenAccept(token ->
                                LastFmAPI.createNewAPI(token, apiKey, apiSecret)
                                        .thenAccept(api -> {
                                            packageConfig.set(SESSION_KEY_NAME, api.getSessionToken());
                                            packageConfig.saveConfig();
                                            completableFuture.complete(lastFmAPI = api);
                                        }))
                        .exceptionally(e -> {
                    completableFuture.completeExceptionally(e);
                    return null;
                }));
        
        return completableFuture;
    }
    
    private CompletableFuture<String> requestSessionToken() {
        try {
            Desktop.getDesktop().browse(URI.create(AUTH_REQUEST_URL.formatted(apiKey)));
        } catch (Exception e) {
            System.out.println("Please open the following in your browser:");
            System.out.println(AUTH_REQUEST_URL.formatted(apiKey));
//            throw new RuntimeException(e);
        }

        var codeFuture = new CompletableFuture<String>();

        try {
            var server = setupCallbackServer(codeFuture);
            server.join();
        } catch (Exception e) {
            codeFuture.completeExceptionally(e);
        }
        
        return codeFuture;
    }

    /**
     * Sets up the server on port 8088 and completes the future with the {@code code} query parameter.
     *
     * @param codeFuture The future to complete with the code
     * @return The created server
     * @throws Exception
     */
    private Server setupCallbackServer(CompletableFuture<String> codeFuture) throws Exception {
        var server = new Server(8088); // Set your desired port
        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) throws Exception {
                if (!request.getHttpURI().getPath().equals("/")) {
                    return false;
                }

                // Not great but works for basic local cases
                var code = request.getHttpURI().getQuery().split("&")[0].split("=")[1];

                System.out.println("request.getHttpURI().getQuery() = " + request.getHttpURI().getQuery());
                System.out.println("code = " + code);

                response.setStatus(200);
                response.write(true, ByteBuffer.wrap("Thank you! You may close this tab.\n".getBytes(StandardCharsets.UTF_8)), callback);

                server.stop();

                codeFuture.complete(code);
                return true;
            }
        });

        server.start();
        return server;
    }

//    private static String getSessionToken(String username, String password) throws IOException, InterruptedException {
//        Map<String, String> params = new HashMap<>();
//        params.put("method", "auth.getMobileSession");
//        params.put("username", username);
//        params.put("password", password);
//        params.put("api_key", API_KEY);
//        params.put("format", "json");
//
//        String apiSig = generateSignature(params);
//        params.put("api_sig", apiSig);
//
//        String response = sendPostRequest(params);
//        return response; // Parse this response to extract the session key
//    }

//    private String getUserInfo() throws IOException, InterruptedException {
//        Map<String, String> params = new HashMap<>();
//        params.put("method", "user.getInfo");
//        params.put("api_key", apiKey);
//        params.put("sk", sessionToken);
//        params.put("format", "json");
//
//        String apiSig = generateSignature(params);
//        params.put("api_sig", apiSig);
//
//        return sendPostRequest(params);
//    }
}
