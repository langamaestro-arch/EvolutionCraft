# Evolution Craft · alfa técnica para NeoForge 26.3

Este proyecto es una **primera implementación parcial**, no una versión final ni un JAR verificado.

## Implementado en código
- Registro de fabricación de herramientas/armaduras por jugador.
- Registro de intercambios con aldeanos.
- Registro de avances originales (sin contar recetas ocultas).
- Conteo de capturas, especies y tesoros de pesca.
- Registro de derrotas del Warden y Wither atribuidas al jugador.
- Evaluación de las seis eras y de los cinco niveles de pesca.
- Expulsión del equipamiento de eras superiores al colocarlo en mano o armadura.
- Persistencia por mundo en `evolutioncraft_players.json`.
- Compilación automática propuesta mediante `.github/workflows/build.yml`.

## No implementado todavía
- Bloqueo efectivo de resultados de recetas **antes** de fabricar (se necesita interceptar el cálculo de salida en mesa de trabajo y herrería). Por ello actualmente se puede fabricar un objeto avanzado y guardarlo.
- Bloqueo de portales Nether/End (los métodos `canNether()` y `canEnd()` están definidos, pero no conectados al transporte dimensional).
- Menú P, notificaciones completas, monumentos y recompensas legendarias.
- Restricciones de la mesa de encantamientos y el soporte de pociones.
- Verificación exhaustiva de requisitos originales y conteos por categorías de avances.
- Pruebas de compilación 26.3, integración, servidor y supervivencia.

## Compilar
Necesitas **JDK 25**, conexión a Internet y Gradle 9.3.1. En la raíz del proyecto: `gradle build`.

Alternativa: subir **todo el contenido de esta carpeta** a un repositorio NUEVO de GitHub. En `Actions`, ejecutar `Compilar Evolution Craft 26.3` y recuperar el artefacto generado, una vez superada la compilación. Los errores se consultan en el log del job.

**Importante:** no instalar el proyecto ZIP en `mods/`; solo se instala el JAR después de compilar y verificarlo.

## Limitaciones conocidas
- Los archivos JSON de progreso son una solución provisional; producción debería usar almacenamiento persistente integrado y recuperación robusta ante corrupción.
- La comprobación de equipamiento se ejecuta cada 5 ticks y debe probarse en todos los accesos a inventario.
- Falta protección contra las recetas alternativas y entidades automatizadas.
- El manejo de biomas para el acceso dimensional y las reglas de especialización deben validarse antes de activar los portales.
