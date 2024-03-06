package dev.chux.gcp.crun;

import static spark.Spark.*;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.UUID;

import com.google.inject.Guice;
import com.google.inject.Injector;

import dev.chux.gcp.crun.rest.RestAPI;

public class App {

    public static void main(String[] args) {
      final Injector injector = Guice.createInjector(new AppModule());
      injector.getInstance(RestAPI.class).serve(8080);
    }

}
