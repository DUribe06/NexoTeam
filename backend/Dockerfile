# Compila y empaqueta el backend; no necesita nada instalado en quien despliega.
FROM eclipse-temurin:17-jdk AS compilar
WORKDIR /codigo
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:17-jre
RUN useradd --system --no-create-home nexo
WORKDIR /app
COPY --from=compilar /codigo/build/libs/*.jar nexo.jar
USER nexo
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "nexo.jar"]
