package berlin.yuna.nano.examples;

import berlin.yuna.typemap.model.TypeMap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.Date;

import static java.net.http.HttpResponse.BodyHandlers.ofString;


class RestCallTest {

    @Test
    void restCall() throws IOException, InterruptedException {

        // Request
        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://dummyjson.com/users/1")).build();
        final TypeMap user = new TypeMap(HttpClient.newHttpClient().send(request, ofString()).body());

        // Results:
        final int age = user.get(Integer.class, "age");
        final float weight = user.get(Float.class, "weight");
        final InetAddress ip = user.get(InetAddress.class, "ip");

        // Date handling:
        user.put("bornAt", new Date().toString());
        final Instant date = user.get(Instant.class, "bornAt");
    }
}
