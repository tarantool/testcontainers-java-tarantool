package org.testcontainers.containers;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public List<HashMap<String, List<String>>> getInstancesName(){
        List<HashMap<String, List<String>>> nods = instances.get().values().stream()
                .map(Noda::new)
                .map(Noda::getRolesInstances)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return nods;
    }

    static class Noda{
        private final String regxStrRouter = "^router";
        private final Pattern patternRouter = Pattern.compile(regxStrRouter, Pattern.CASE_INSENSITIVE);
        private boolean router;
        private Set<String> instances;
        private List<String> roles;
        private Integer weight;
        private boolean allRW;
        private String vshardGroup;

        public Noda(Map<String, Object> map){
            List<String> x = (List<String>) map.get("instances");
            this.instances = new HashSet<String>(x);
            this.router = this.instances.stream().filter(i -> patternRouter.matcher(i).matches()).count() > 0 ? true : false;
            if(!this.router){
                this.weight = (Integer) map.get("weight");
                this.vshardGroup = (String) map.get("vshard_group");
            }
            this.roles = (List<String>) map.get("roles");
            this.allRW = (boolean) map.get("all_rw");
        }

        public HashMap<String, List<String>> getRolesInstances(){
            HashMap<String, List<String>> map = new HashMap<String, List<String>>();
            for(String instan : this.instances){
                map.put(instan,this.roles);
            }
            return map;
        }

    }

}
