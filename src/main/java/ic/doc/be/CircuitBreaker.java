package ic.doc.be;

import ic.doc.web.ApiResponse;
import org.apache.http.client.fluent.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CircuitBreaker {

    private static final int DEFAULT_PORT = 5002;
    private static final String WEATHER_URI = "http://localhost:5053/";

    public CircuitBreaker(int port) throws Exception {
        Server server = new Server(port);
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(new ServletHolder(new CircuitBreaker.Website()), "/");
        server.setHandler(handler);

        server.start();
    }

    static class Website extends HttpServlet {
        private int numberOfRequests = 0;
        private long lastRequestTime = 0;
        private final long DELAY = 14000;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            long timeSinceLastRequest = currentTime() - lastRequestTime;
            long timeRemaining = DELAY - timeSinceLastRequest;
            boolean isBroken = timeSinceLastRequest < DELAY;

            numberOfRequests++;
            // if true, forward the request, if the system is overloaded it will break
            if (!isBroken) {
                lastRequestTime = currentTime();
                numberOfRequests = 1;
                new ApiResponse(fetchDataFrom(WEATHER_URI)).writeTo(resp);
            } else {
                new ApiResponse("Latest temp forecast: " + "[wasn't able to retrieve forecast data]\n" + "You attempted " + numberOfRequests + " while the system is still overloaded. Retry in " + timeRemaining + "ms").writeTo(resp);
            }
        }

        private String fetchDataFrom(String newsUri) {
            try {
                return Request.Get(newsUri).execute().returnContent().asString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private long currentTime() {
            return System.currentTimeMillis();
        }
    }

    private static int portFrom(String[] args) {
        if (args.length < 1) {
            return DEFAULT_PORT;
        }
        return Integer.valueOf(args[0]);
    }

    public static void main(String[] args) throws Exception {
        new CircuitBreaker(portFrom(args));
    }
}