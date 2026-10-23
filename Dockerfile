# =====================================================================
# Multi-Stage Dockerfile for Personal Expense Tracker
# Builds with Ant / OpenJDK and deploys directly to Apache Tomcat 9
# Compatible with Render's dynamic $PORT environment variable
# =====================================================================

# Stage 1: Build application WAR
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Install Ant, curl, and ca-certificates
RUN apk add --no-cache apache-ant curl ca-certificates

# Copy project files
COPY . .

# Ensure lib and WebContent/WEB-INF/lib exist and fetch dependencies if missing
RUN mkdir -p lib WebContent/WEB-INF/lib && \
    [ -f lib/javax.servlet-api-4.0.1.jar ] || curl -fsSL -o lib/javax.servlet-api-4.0.1.jar https://repo1.maven.org/maven2/javax/servlet/javax.servlet-api/4.0.1/javax.servlet-api-4.0.1.jar && \
    [ -f lib/mysql-connector-j-8.3.0.jar ] || curl -fsSL -o lib/mysql-connector-j-8.3.0.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar && \
    [ -f lib/gson-2.10.1.jar ] || curl -fsSL -o lib/gson-2.10.1.jar https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar && \
    cp -u lib/mysql-connector-j-8.3.0.jar WebContent/WEB-INF/lib/ && \
    cp -u lib/gson-2.10.1.jar WebContent/WEB-INF/lib/

# Run Ant WAR packaging (with standard javac/jar fallback)
RUN ant war || ( \
    mkdir -p build/classes dist build/war_temp && \
    find src -name "*.java" > sources.txt && \
    javac -cp "lib/*" -d build/classes -encoding UTF-8 @sources.txt && \
    cp src/db.properties build/classes/ && \
    cp -r WebContent/* build/war_temp/ && \
    cp -r build/classes build/war_temp/WEB-INF/ && \
    cd build/war_temp && jar -cvf ../../dist/PersonalExpenseTracker.war * \
)

# Stage 2: Production Apache Tomcat 9 Runtime
FROM tomcat:9.0-jdk17-temurin

# Clean default Tomcat webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Set default application mode to demo (can be overridden to 'database' at runtime)
ENV APP_MODE=demo

# Disable Tomcat shutdown port (prevents cloud health-checks from hitting the shutdown socket)
RUN sed -i 's/<Server port="8005"/<Server port="-1"/' /usr/local/tomcat/conf/server.xml

# Enable RemoteIpValve for Render HTTPS reverse proxy and client IP forwarding
RUN sed -i '/<\/Host>/i \        <Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="x-forwarded-for" protocolHeader="x-forwarded-proto" protocolHeaderHttpsValue="https" />' /usr/local/tomcat/conf/server.xml

# Copy WAR as ROOT.war for root domain mapping (http://domain.com/ instead of /PersonalExpenseTracker/)
COPY --from=builder /app/dist/PersonalExpenseTracker.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

# Dynamically bind HTTP connector to cloud provider's $PORT (Render default) or fallback to 8080
CMD ["sh", "-c", "TARGET_PORT=\"${PORT:-8080}\" && echo \"[Startup] Binding Tomcat HTTP connector to port: $TARGET_PORT\" && sed -i \"s/<Connector port=\\\"[0-9]*\\\"/<Connector port=\\\"$TARGET_PORT\\\"/g\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
