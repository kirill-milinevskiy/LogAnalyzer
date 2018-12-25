package com.km;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.err;
import static java.lang.System.out;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class Main {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss,SSS");
    private static final String METHOD_ID_REGEX = "[a-zA-Z]+:[0-9]+";
    private static final Pattern METHOD_ID_PATTERN = Pattern.compile(METHOD_ID_REGEX);

    public static void main(String[] args) {
        long start = System.nanoTime();

        if (args.length == 0) {
            out.println("File not passed as an argument. Nothing to calculate.");
        }
        analyze(args[0]);

        out.println("\n\ntotal time: " + (System.nanoTime() - start) / 1000000 + "ms");
    }

    private static void analyze(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            Map<String, MethodStat> statMap = new TreeMap<>();
            Map<String, String> entryTimeStamps = new TreeMap<>();

            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("entry with (")) {
                    String timeStamp = getTimeStamp(line);
                    getMethodIdFromLine(line).ifPresent(m -> entryTimeStamps.put(m, timeStamp));
                } else if (line.contains("exit with (")) {
                    final String finalLine = line;
                    getMethodIdFromLine(line).ifPresent(mId -> {
                        String methodName = getMethodNameFromMethodId(mId);
                        MethodStat stat = getUpdatedStat(entryTimeStamps, statMap, mId, finalLine);
                        statMap.put(methodName, stat);
                    });
                }
            }

            statMap.forEach((mn, stat) -> out.println("\n\n" + mn + "\n" + stat));
        } catch (IOException e) {
            err.println("Something went wrong!");
            e.printStackTrace();
        }
    }

    private static MethodStat getUpdatedStat(Map<String, String> entryTimeStamps, Map<String, MethodStat> statMap,
                                             String methodId, String line) {
        String methodName = getMethodNameFromMethodId(methodId);

        String entryTimeStamp = entryTimeStamps.remove(methodId);
        String exitTimeStamp = getTimeStamp(line);
        Long evalTime = getEvalTime(entryTimeStamp, exitTimeStamp);

        MethodStat stat = statMap.getOrDefault(methodName, new MethodStat());
        return getUpdatedStat(stat, evalTime, getCallIdFromMethodId(methodId));
    }

    private static Long getEvalTime(String entryTimeStamp, String exitTimeStamp) {
        return ChronoUnit.MILLIS.between(LocalDateTime.parse(entryTimeStamp, formatter),
                LocalDateTime.parse(exitTimeStamp, formatter));
    }

    private static String getTimeStamp(String line) {
        return line.split(" ")[0];
    }

    private static MethodStat getUpdatedStat(MethodStat stat, Long evalTime, String callId) {
        MethodStat updated = new MethodStat()
                .setCount(stat.getCount() + 1)
                .setTimeSum(stat.getTimeSum() + evalTime)
                .setMinTime(evalTime < stat.getMinTime() ? evalTime : stat.getMinTime());

        if (evalTime > stat.getMaxTime()) {
            updated.setMaxTimeId(callId)
                    .setMaxTime(evalTime);
        } else {
            updated.setMaxTimeId(stat.getMaxTimeId())
                    .setMaxTime(stat.getMaxTime());
        }

        return updated;
    }

    private static Optional<String> getMethodIdFromLine(String line) {
        Matcher matcher = METHOD_ID_PATTERN.matcher(line);
        return matcher.find() ? of(matcher.group(0)) : empty();
    }

    private static String getMethodNameFromMethodId(String methodId) {
        return methodId.split(":")[0];
    }

    private static String getCallIdFromMethodId(String methodId) {
        return methodId.split(":")[1];
    }

    private static class MethodStat {

        private Long minTime = Long.MAX_VALUE;
        private Long maxTime = 0L;
        private Long timeSum = 0L;
        private Long count = 0L;
        private String maxTimeId;

        public Long getMinTime() {
            return minTime;
        }

        public MethodStat setMinTime(Long minTime) {
            this.minTime = minTime;
            return this;
        }

        public Long getMaxTime() {
            return maxTime;
        }

        public MethodStat setMaxTime(Long maxTime) {
            this.maxTime = maxTime;
            return this;
        }

        public Long getTimeSum() {
            return timeSum;
        }

        public MethodStat setTimeSum(Long timeSum) {
            this.timeSum = timeSum;
            return this;
        }

        public Long getCount() {
            return count;
        }

        public MethodStat setCount(Long count) {
            this.count = count;
            return this;
        }

        public String getMaxTimeId() {
            return maxTimeId;
        }

        public MethodStat setMaxTimeId(String maxTimeId) {
            this.maxTimeId = maxTimeId;
            return this;
        }

        @Override
        public String toString() {
            return "Method stat: " +
                    "minTime=" + minTime +
                    ", maxTime=" + maxTime +
                    ", average=" + timeSum / count +
                    ", count=" + count +
                    ", maxTimeId='" + maxTimeId + '\'';
        }
    }
}
