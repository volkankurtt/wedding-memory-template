# HEIC/HEIF conversion must work in this image. Do not assume ImageMagick is on the host.
FROM eclipse-temurin:17-jdk-noble AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-noble
RUN apt-get update \
	&& apt-get install -y --no-install-recommends \
		imagemagick \
		libheif1 \
		libheif-plugin-aomdec \
		libheif-plugin-aomenc \
		libheif-plugin-libde265 \
	&& rm -rf /var/lib/apt/lists/* \
	&& if [ -f /etc/ImageMagick-7/policy.xml ]; then \
		sed -i 's/rights="none" pattern="HEIC"/rights="read|write" pattern="HEIC"/g; s/rights="none" pattern="HEIF"/rights="read|write" pattern="HEIF"/g' /etc/ImageMagick-7/policy.xml; \
	fi \
	&& if [ -f /etc/ImageMagick-6/policy.xml ]; then \
		sed -i 's/rights="none" pattern="HEIC"/rights="read|write" pattern="HEIC"/g; s/rights="none" pattern="HEIF"/rights="read|write" pattern="HEIF"/g' /etc/ImageMagick-6/policy.xml; \
	fi
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
