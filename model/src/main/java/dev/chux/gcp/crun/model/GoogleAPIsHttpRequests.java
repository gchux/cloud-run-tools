package dev.chux.gcp.crun.model;

import java.util.List;

public class GoogleAPIsHttpRequests extends Multivalue<GoogleAPIsHttpRequest> {

  GoogleAPIsHttpRequests() {}

  public GoogleAPIsHttpRequests(final List<GoogleAPIsHttpRequest> requests) {
    super(requests);
  }

}
