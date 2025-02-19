package dev.chux.gcp.crun.model;

import java.util.List;

public class HttpRequests extends Multivalue<HttpRequest> {

  HttpRequests() {}

  public HttpRequests(final List<HttpRequest> requests) {
    super(requests);
  }

}
