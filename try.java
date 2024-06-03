import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class KafkaKubectlOperations {

    public static void main(String[] args) {
        try {
            // Example: Apply a Kubernetes configuration file
            runKubectlApplyCommand("/path/to/your/kafka-config.yaml");

            // Execute `kubectl exec` to create a Kafka topic inside the broker pod
            runKubectlExecCommand("broker-1", "kafka-topics --create --topic test-topic --bootstrap-server kafka-0.kafka:9092,kafka-1.kafka:9092,kafka-2.kafka:9092 --replication-factor 3 --partitions 1");
            
            // Execute `kubectl exec` to produce messages to the Kafka topic inside the broker pod
            runKubectlExecCommand("broker-1", "sh -c 'seq 10 | kafka-console-producer --broker-list kafka-0.kafka:9092,kafka-1.kafka:9092,kafka-2.kafka:9092 --topic test-topic'");

            // Execute `kubectl exec` to consume messages from the Kafka topic inside the broker pod
            runKubectlExecCommand("broker-1", "kafka-console-consumer --bootstrap-server kafka-0.kafka:9092,kafka-1.kafka:9092,kafka-2.kafka:9092 --topic test-topic --from-beginning --timeout-ms 10000");

            // Example kubectl command: Listing pods (outside of Kafka broker pod for reference)
            runShellCommand("kubectl get pods");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runKubectlApplyCommand(String filePath) throws IOException, InterruptedException {
        String kubectlCommand = String.format("kubectl apply -f %s", filePath);
        runShellCommand(kubectlCommand);
    }

    private static void runKubectlExecCommand(String podName, String command) throws IOException, InterruptedException {
        String kubectlCommand = String.format("kubectl exec %s -- %s", podName, command);
        runShellCommand(kubectlCommand);
    }

    private static void runShellCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        if (exitCode != 0) {
            System.out.println("Command failed with exit code: " + exitCode);
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    System.err.println(errorLine);
                }
            }
        }
    }
}
