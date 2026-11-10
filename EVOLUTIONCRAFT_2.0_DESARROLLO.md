# EvolutionCraft 2.0 — beta 1 para pruebas

**Rama:** `evolutioncraft-2.0`. La rama `main` sigue siendo la versión estable previa.

## Sistemas implementados

- **Eras:** seis niveles (Madera, Piedra y cuero, Cobre, Hierro, Diamante, Netherita), herramientas, equipo, comercio, misiones y maestría. Las herramientas adelantadas se conservan en el inventario; el servidor bloquea interacciones si no hay huecos.
- **Habilidades:** Pesca, Agricultura, Ganadería, Exploración, Herramientas y Alquimia, cada una con cinco niveles.
- **Exploración:** biomas de Overworld/Nether, aldeas y estructuras visitadas personalmente.
- **Misiones especiales:** primera noche + hoguera; aldea + comercio; 32 menas de cobre; cinco menas de diamante + estructura profunda; Nether + escombro ancestral; equipamiento de netherita y herramientas V.
- **Herramientas/minería:** cuenta tipos y usos; el registro persistente de bloques colocados impide que minerales colocados por jugadores se contabilicen como naturales.
- **Alquimia:** registra pociones extraídas del soporte, efectos únicos, tandas y formatos prolongado, potenciado, arrojadizo y persistente.
- **Encantamientos:** mesa desbloqueada desde Hierro, límites de encantamientos efectivos según era (sin reescribir el objeto ni destruirlo).
- **Dimensiones:** Nether requiere Diamante, 40 avances originales, 20 biomas, Exploración IV, Pesca/Agricultura/Ganadería III y Warden; End requiere 70 avances originales, Exploración V, Alquimia III, tres habilidades V, Wither y la exploración del Nether.
- **Avances nativos:** 59 definiciones JSON con pantallas de avances y toasts para habilidades, eras, misiones, portales y 15 secretos.
- **Secretos:** 15 logros opcionales, detección de las condiciones y distintivos equipables mediante botón en la guía P o comando `/ecdistintivo <id>`.
- **Interfaz P:** tres grupos, Evolución / Habilidades / Logros, iconos de objetos, tarjetas y barras temáticas, texto sin truncar y paginación dinámica.

## Pruebas pendientes en Minecraft 26.3

GitHub Actions verifica la **compilación Java 25 + NeoForge**, pero NO equivale a pruebas funcionales. Antes de promocionar esta rama a `main`, probar especialmente:

1. Creación de mundo y carga de todas las 59 definiciones de avance; comprobar notificaciones de nivel y ausencia de avances propios en el contador original.
2. Restricción de encantamientos para espada, arco, herramientas y armadura en cada era, en servidor y con objetos hallados en cofres.
3. Elaboración de varias tandas de pociones, recogidas con y sin pantalla abierta. El contador agrupa extracciones cercanas en el tiempo (aproximación de 20 ticks).
4. Los 15 desafíos opcionales, sus disparadores y el botón de selección de distintivos.
5. Probar bloqueo Nether con 39/40 avances y End con 69/70. Revisar si 70 avances originales son factibles antes de entrar al End.
6. Una partida larga: guardado/reinicio, inventario lleno, minería natural/colocada, noches sin dormir y dos jugadores simultáneos.
7. Renderizado de la interfaz en resoluciones pequeñas; el diseño de escritorio requiere comprobar adaptabilidad.

**Estado:** Beta de pruebas, **no versión final validada en partida**. No sustituir el JAR estable en una partida importante sin copia de seguridad.
