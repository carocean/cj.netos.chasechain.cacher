package cj.netos.chasechain.cacher;

import cj.studio.ecm.IServiceSite;

import java.util.HashMap;
import java.util.Map;

public class CacherCommandSelector implements ICacherCommandSelector {
    IServiceSite site;
    Map<String, ICacherCommand> commandMap;
    ICacherCommandBuilder commandBuilder;

    public CacherCommandSelector(IServiceSite site, ICacherCommandBuilder commandBuilder) {
        this.site = site;
        commandMap = new HashMap<>();
        this.commandBuilder = commandBuilder;
    }

    @Override
    public synchronized ICacherCommand select(String key) {
        if (commandMap.containsKey(key)) {
            return commandMap.get(key);
        }
        ICacherCommand command=commandBuilder.create(key);
        commandMap.put(key, command);
        return command;
    }
}
