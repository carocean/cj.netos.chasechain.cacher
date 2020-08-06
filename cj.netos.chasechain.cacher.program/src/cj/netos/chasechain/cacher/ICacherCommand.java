package cj.netos.chasechain.cacher;

import cj.studio.ecm.net.CircuitException;

public interface ICacherCommand {
    void doCommand(CacherEvent cacherEvent) throws CircuitException;
}
