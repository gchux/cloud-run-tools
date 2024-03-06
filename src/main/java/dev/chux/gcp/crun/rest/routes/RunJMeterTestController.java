package dev.chux.gcp.crun.rest.routes;

import spark.Request;
import spark.Response;
import javax.servlet.ServletOutputStream;
import java.util.UUID;
import com.google.inject.Inject;
import dev.chux.gcp.crun.jmeter.JMeterTestService;

public class RunJMeterTestController implements spark.Route {

  private static final String SYS_OUT = "sys";

  final JMeterTestService jMeterTestService;

  @Inject
  public RunJMeterTestController(JMeterTestService jMeterTestService) {
    this.jMeterTestService = jMeterTestService;
  }

  public Object handle(Request request, Response response) throws Exception {

    final String testId = UUID.randomUUID().toString();
    final String output = request.queryParams("output"); 
    final ServletOutputStream responseOutput = response.raw().getOutputStream();

    response.type("text/plain");
    response.header("x-jmeter-test", testId);
    responseOutput.println("---- started: " + testId + " ----");

    final String host = request.queryParams("host");
    final String path = request.queryParams("path");
    final int concurrency = Integer.parseInt(request.queryParams("concurrency"), 10);
    final int duration = Integer.parseInt(request.queryParams("duration"), 10);
    final int rampupTime = Integer.parseInt(request.queryParams("rampup_time"), 10);
    final int rampupSteps = Integer.parseInt(request.queryParams("rampup_steps"), 10);

    if( output != null && output.equalsIgnoreCase(SYS_OUT) ) {
      this.jMeterTestService.start(host, path, concurrency, duration, rampupTime, rampupSteps);
    } else {
      this.jMeterTestService.start(host, path, concurrency, duration, 
          rampupTime, rampupSteps, responseOutput, false /* closeable */);
    }
    
    return "---- finished: " + testId + " ----";
  }

}
