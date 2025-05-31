# check=skip=SecretsUsedInArgOrEnv
# ^ disabling this warning because the JOOQ_DB_PASSWORD is not actually a secret. It is a temporary database that gets destroyed
# as soon as the build process ends and only exists to run flyway migrations.
# Step 1: build the app JAR.
# Because the generateJooq task requires a database connection AND we cannot start a testcontainer in a docker build,
# we need to find another way. Use a postgres container to start a local db and build the app there instead.
FROM postgres:15-bookworm AS jooqbox
RUN apt-get update && apt-get install -y wget # install wget
RUN wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb # fetch a JDK (oracle in this case)
RUN apt install ./jdk-21_linux-x64_bin.deb # install
# Now, set up a local database
ENV DB_NAME=github_ai_assistant
ENV JOOQ_DB_URL=jdbc:postgresql://localhost:5432/$DB_NAME
ENV JOOQ_DB_USERNAME=jooquser
ENV JOOQ_DB_PASSWORD=jooqPassword
# Copy source code into container
WORKDIR /app
COPY . /app
RUN chown -R postgres:postgres /app
# Initialize the database cluster
RUN mkdir -p /var/run/postgresql && chown -R postgres:postgres /var/run/postgresql
USER postgres
RUN initdb -D /var/lib/postgresql/data
# Start a postgres server, create a database, run the migrations and build the app
RUN pg_ctl -D /var/lib/postgresql/data -l /var/lib/postgresql/data/logfile start && \
    pg_isready -h localhost -p 5432 -U postgres && \
    psql -c "CREATE USER $JOOQ_DB_USERNAME WITH PASSWORD '$JOOQ_DB_PASSWORD';" && \
    psql -c "CREATE DATABASE $DB_NAME;" && \
    psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $JOOQ_DB_USERNAME;" && \
    ./gradlew generateJooq && ./gradlew assemble

# Switch to a lightweight image for running the app (JRE only, no JDK)

FROM eclipse-temurin:21-jre-ubi9-minimal AS runner

LABEL org.opencontainers.image.source=https://github.com/GPIG-group-B-2024/gitHub-ai-assistant
LABEL org.opencontainers.image.description="A Github app that automatically produces solutions to pull requests"

COPY --from=jooqbox app/build/libs/ai-assistant-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar", "-Xms512m", "-Xmx512m"]