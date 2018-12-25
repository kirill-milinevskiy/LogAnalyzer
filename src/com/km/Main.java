package com.km;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TreeMap;

public class Main {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss,SSS");

    private static final String METHOD_ID_REGEX = "[^+0-9a-zA-Z:]+";

    public static void main(String[] args) {
        long start = System.nanoTime();
        String filePath = args[0];

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            TreeMap<String, MethodStat> statMap = new TreeMap<>();
            TreeMap<String, String> entries = new TreeMap<>();

            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("entry with (")) {
                    String[] tokens = line.split(" ");
                    String entryToken = tokens[5];
                    String timeStamp = tokens[0];
                    entries.put(entryToken.replaceAll(METHOD_ID_REGEX, ""), timeStamp);
                } else if (line.contains("exit with (")) {
                    String[] tokens = line.split(" ");
                    String exitToken = tokens[5];

                    if (!exitToken.contains(":")) continue;

                    String methodName = exitToken.substring(1, exitToken.lastIndexOf(":"));

                    String entryTimeStamp = entries.remove(exitToken.replaceAll(METHOD_ID_REGEX, ""));

                    if (entryTimeStamp == null) continue;

                    String exitTimeStamp = tokens[0];

                    Long evalTime = ChronoUnit.MILLIS.between(LocalDateTime.parse(entryTimeStamp, formatter), LocalDateTime.parse(exitTimeStamp, formatter));

                    MethodStat stat;
                    if (!statMap.containsKey(methodName)) {
                        stat = new MethodStat();
                        statMap.put(methodName, stat);
                    } else
                        stat = statMap.get(methodName);

                    if (evalTime > stat.getMaxTime()) {
                        stat.setMaxTime(evalTime);
                        stat.setMaxTimeId(exitToken.substring(exitToken.lastIndexOf(":") + 1).replaceAll(METHOD_ID_REGEX, ""));
                    }

                    if (evalTime < stat.getMinTime())
                        stat.setMinTime(evalTime);

                    stat.setCount(stat.getCount() + 1);
                    stat.setTimeSum(stat.getTimeSum() + evalTime);
                }
            }

            statMap.forEach((mn, stat) -> System.out.println("\n\n" + mn + "\n" + stat));
            System.out.println("\n\ntotal time: " + (System.nanoTime() - start) / 1000000 + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class MethodStat {

        private Long minTime = 0L;
        private Long maxTime = 0L;
        private Long timeSum = 0L;
        private Long count = 0L;
        private String maxTimeId;

        public Long getMinTime() {
            return minTime;
        }

        public void setMinTime(Long minTime) {
            this.minTime = minTime;
        }

        public Long getMaxTime() {
            return maxTime;
        }

        public void setMaxTime(Long maxTime) {
            this.maxTime = maxTime;
        }

        public Long getTimeSum() {
            return timeSum;
        }

        public void setTimeSum(Long timeSum) {
            this.timeSum = timeSum;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public String getMaxTimeId() {
            return maxTimeId;
        }

        public void setMaxTimeId(String maxTimeId) {
            this.maxTimeId = maxTimeId;
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
