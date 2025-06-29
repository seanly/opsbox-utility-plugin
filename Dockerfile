FROM seanly/toolset:openjdk8u372 AS build

COPY ./ /code
WORKDIR /code

RUN ./mvnw clean package

FROM seanly/scratch

COPY --from=build /code/target/opsbox-utility-plugin.hpi /target/