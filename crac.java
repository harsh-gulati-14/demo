import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import jdk.crac.Core;
import jdk.crac.Resource;
import jdk.crac.CheckpointException;
import jdk.crac.RestoreException;
import java.time.Instant;

public class LambdaHandler implements RequestHandler<Object, String>, Resource {

    private static String cachedData;
    private static Instant lastUpdateTime;
    private static final long DATA_REFRESH_INTERVAL = 10 * 60; // 10 minutes in seconds

    static {
        // Register the LambdaHandler as a CRaC resource to manage checkpoints
        Core.getGlobalContext().register(new LambdaHandler());
    }

    public LambdaHandler() {
        // Perform initialization only once before the checkpoint
        if (cachedData == null || isDataExpired()) {
            System.out.println("First initialization or data expired: Fetching external data...");
            cachedData = fetchDataFromApi();  // Fetch new data from the API
            lastUpdateTime = Instant.now();   // Record the time of the data update
        }
    }

    @Override
    public String handleRequest(Object input, Context context) {
        // Check if the data has expired before processing the request
        if (isDataExpired()) {
            System.out.println("Data expired: Fetching fresh data...");
            cachedData = fetchDataFromApi();  // Fetch fresh data
            lastUpdateTime = Instant.now();   // Update the last refresh time
            // Ideally, you would trigger a new CRaC checkpoint after fetching new data
        }

        // Process the request using the cached (or freshly fetched) data
        return "Lambda executed with cached data: " + cachedData;
    }

    private String fetchDataFromApi() {
        // Simulate an external HTTP call to fetch data
        return "External API response data";
    }

    private boolean isDataExpired() {
        // Check if the last data update was more than 10 minutes ago
        if (lastUpdateTime == null) return true;
        long timeSinceLastUpdate = Instant.now().getEpochSecond() - lastUpdateTime.getEpochSecond();
        return timeSinceLastUpdate > DATA_REFRESH_INTERVAL;
    }

    @Override
    public void beforeCheckpoint(CheckpointException e) throws Exception {
        // Code to run before a checkpoint is taken
        System.out.println("Taking a checkpoint with data: " + cachedData);
    }

    @Override
    public void afterRestore(RestoreException e) throws Exception {
        // Code to run after restoring from a checkpoint
        System.out.println("Restored from checkpoint. Cached data: " + cachedData);
        // Check if data is still fresh after restore
        if (isDataExpired()) {
            System.out.println("Data expired after restore. Fetching fresh data...");
            cachedData = fetchDataFromApi();
            lastUpdateTime = Instant.now();
        }
    }
}
