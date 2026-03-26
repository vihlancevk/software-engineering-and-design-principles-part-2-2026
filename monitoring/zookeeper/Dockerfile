FROM zookeeper:3.8

# Download JMX Prometheus Java agent to expose ZooKeeper JVM metrics
RUN apt-get update -qq && apt-get install -y --no-install-recommends wget \
    && wget -q -O /jmx_prometheus_javaagent.jar \
       https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.20.0/jmx_prometheus_javaagent-0.20.0.jar \
    && apt-get purge -y wget && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

COPY jmx-config.yml /jmx-config.yml

# Start JMX exporter HTTP server on port 7070 alongside ZooKeeper
ENV JVMFLAGS="-javaagent:/jmx_prometheus_javaagent.jar=7070:/jmx-config.yml"
