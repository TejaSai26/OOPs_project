#!/bin/sh
# Brings the Campus Resource Hub back after a sandbox restart.
set -e
cd /workspace/crsms
if curl -sf -o /dev/null --max-time 2 http://127.0.0.1:8080/api/health; then
  exit 0
fi
mkdir -p data/uploads /workspace/.tools
if [ ! -x /workspace/.tools/maven/bin/mvn ]; then
  if [ -x /opt/apache-maven-3.9.9/bin/mvn ]; then
    cp -a /opt/apache-maven-3.9.9 /workspace/.tools/maven
  else
    curl -fsSL -o /tmp/maven.tgz https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz
    tar -xzf /tmp/maven.tgz -C /workspace/.tools
    mv /workspace/.tools/apache-maven-3.9.9 /workspace/.tools/maven
  fi
fi
export PATH="/workspace/.tools/maven/bin:$PATH"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
export ADMIN_PASSWORD="${ADMIN_PASSWORD:-changeme}"
nohup mvn -q -DskipTests spring-boot:run > /tmp/crsms.log 2>&1 &
i=0
while [ "$i" -lt 90 ]; do
  if curl -sf -o /dev/null --max-time 2 http://127.0.0.1:8080/api/health; then
    exit 0
  fi
  i=$((i + 1))
  sleep 1
done
echo "Campus Resource Hub did not become healthy" >&2
exit 1
