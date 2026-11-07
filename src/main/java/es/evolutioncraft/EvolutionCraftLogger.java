package es.evolutioncraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EvolutionCraftLogger {
    private static final Logger LOG = LoggerFactory.getLogger("EvolutionCraft");
    private EvolutionCraftLogger() {}
    static void warn(String message) { LOG.warn(message); }
}
