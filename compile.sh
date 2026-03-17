script_dir=$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)
cd $script_dir

clj -T:build uber

exit=$?
if [ $exit -ne 0 ]; then
  echo "Build failed with exit code $exit"
  exit $exit
fi

native-image -jar target/nrepl-client.jar \
  -H:ConfigurationFileDirectories=native-configs \
  -o nrepl-client \
  --no-fallback \
  -H:-CheckToolchain \
  --initialize-at-build-time \
  --initialize-at-run-time=clojure.core.server__init,clojure.pprint.dispatch__init,clojure.pprint__init,clojure.stacktrace__init,org.jline.nativ
