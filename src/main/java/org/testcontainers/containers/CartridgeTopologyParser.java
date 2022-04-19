package org.testcontainers.containers;

import org.yaml.snakeyaml.Yaml;
import sun.invoke.empty.Empty;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CartridgeTopologyParser {

    private final AtomicReference<Map<String, Map<String, Object>>> instances = new AtomicReference<>();

    public CartridgeTopologyParser(String replicasetsFileName) {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(replicasetsFileName);
        instances.set(Collections.unmodifiableMap(yaml.load(inputStream)));
    }

    public void getInstancesName(){
        List<HashMap<String, String[]>> nods = instances.get().values().stream()
                .map(Noda::new)
                .map(Noda::getRolesNod)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    static class Noda{
        private final String regxStrRouter = "^router";
        private final String regexStrStorage = "^s.";
        private final Pattern patternRouter = Pattern.compile(regxStrRouter, Pattern.CASE_INSENSITIVE);
        private final Pattern patternStorage = Pattern.compile(regexStrStorage, Pattern.CASE_INSENSITIVE);
        private String noda;
        private String[] instances;
        private String[] roles;
        private Integer weight;
        private boolean allRW;
        private String vshardGroup;

        public Noda(Map<String, Object> map){

            this.noda = (map.get(patternRouter) != null) ? (String) map.get(patternRouter): (String) map.get(patternStorage);
            if(patternStorage.matcher(this.noda).matches()){
                this.weight = (Integer) map.get("");
            }
            this.instances = (String[]) map.get("instances");
            this.roles = (String[]) map.get("roles");
            this.allRW = (boolean) map.get("all_rw");
            this.vshardGroup = (String) map.get("vshard_group");
        }

        public HashMap<String, String[]> getRolesNod(){
            HashMap<String, String[]> map = new HashMap<String, String[]>();
            map.put(noda, roles);
            return map;
        }

    }

}
