# EvolutionCraft 2.1 Alpha 1 — configuración y optimización

Esta rama parte de **EvolutionCraft 2.0 Beta 10** y no sustituye la versión probada.

## Configuración por mundo

Después de arrancar una partida con la Alpha 1, se crea automáticamente
`evolutioncraft-config.properties` en la carpeta raíz del mundo
(al lado de `evolutioncraft_players.json`).

**Detén el servidor o sal del mundo antes de modificarlo.** Los cambios
se leen al iniciar nuevamente. Se conservan las claves ya definidas.

| Clave | Valor inicial | Descripción |
|---|---:|---|
| `nether.advancements` | 40 | Avances originales para acceder al Nether |
| `end.advancements` | 70 | Avances originales para acceder al End |
| `nether.overworld_biomes` | 20 | Biomas del Overworld para acceder al Nether |
| `performance.scan_interval_ticks` | 80 | Periodicidad de búsqueda de biomas y estructuras (40–400) |
| `rewards.enabled` | true | Permitir reclamar premios |
| `equipment.restrictions` | true | Bloquear uso de equipo y mochilas fuera de su era |
| `crafting.restrictions` | true | Bloquear fabricación anticipada en mods compatibles |
| `arsenal_forge.restrictions` | true | Exigir Hierro para la mesa de Arsenal Forge |

Los mínimos y máximos se validan en el servidor. El progreso y los
premios reclamados no se reinician al modificar estos parámetros.

## Optimización y seguridad

- Evita consultas repetidas de estructuras cuando el jugador está inmóvil.
- Permite espaciar los escaneos, sin omitir el registro de biomas y noches.
- Guarda los datos mediante archivo temporal y sustitución atómica cuando el
  sistema de archivos lo admite, para evitar JSON truncado.
- Conserva las mecánicas actuales y el sistema de recompensas.

## Fases previstas

- Alpha 2: diario de descubrimientos y fichas de exploración.
- Beta 1: contratos opcionales de aventura con progreso por jugador.

**Prueba con una copia de tu mundo**, ya que la Alpha 1 aún no está validada
en juego ni con todos los mods del pack.
