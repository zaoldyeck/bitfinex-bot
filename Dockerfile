FROM hseeberger/scala-sbt:11.0.8_1.4.0_2.13.3
COPY . .
RUN sbt assembly
CMD java -jar ./target/scala-2.13/bitfinex-bot-assembly-1.0.jar