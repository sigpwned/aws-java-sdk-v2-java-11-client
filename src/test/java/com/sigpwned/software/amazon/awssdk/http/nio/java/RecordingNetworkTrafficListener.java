package com.sigpwned.software.amazon.awssdk.http.nio.java;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.github.tomakehurst.wiremock.http.trafficlistener.WiremockNetworkTrafficListener;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RecordingNetworkTrafficListener implements WiremockNetworkTrafficListener {

  private final StringBuilder requests = new StringBuilder();
  private final StringBuilder response = new StringBuilder();

  @Override
  public void opened(Socket socket) {
  }

  @Override
  public void incoming(Socket socket, ByteBuffer byteBuffer) {
    requests.append(StandardCharsets.UTF_8.decode(byteBuffer));
  }

  @Override
  public void outgoing(Socket socket, ByteBuffer byteBuffer) {
    response.append(UTF_8.decode(byteBuffer));
  }

  @Override
  public void closed(Socket socket) {
  }

  public void reset() {
    requests.setLength(0);
  }

  public String getRequests() {
    return requests.toString();
  }

  public String getResponse() {
    return response.toString();
  }
}
