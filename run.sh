cd /home/kanonen/kanonen
/usr/bin/java \
  -Dstaticfiles=./src/main/resources/static \
  -Dport=80 \
  -Dpins=. \
  -Dsequences=sequences \
  -Dlogback.configurationFile=src/main/resources/logback-file.xml \
  -cp build/libs/kanonen-0.0.1.jar:$(find build/install | tr '\n' ':') \
  se.smasseman.kanonen.web.ApplicationKt &
