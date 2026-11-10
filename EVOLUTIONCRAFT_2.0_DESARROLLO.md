# EvolutionCraft 2.0 — estado de desarrollo

**Rama:** `evolutioncraft-2.0`. La rama `main` se mantiene estable y sin cambios de la actualización 2.0.

## Implementado en esta rama (primera fase)
- Estructura persistente por jugador para biomas, estructuras, minería, herramientas, alquimia, misiones y desafíos.
- Registro periódico de biomas de Overworld y Nether y detección de estructuras en servidor.
- Registro de bloques rotos con herramientas por material y tipo; minerales de cobre y diamante.
- Misiones iniciales de hoguera/primera noche, aldea/comercio, cobre y exploración de estructura profunda.
- Requisito Nether: era Diamante, 40 avances originales, 20 biomas, Exploración IV, Pesca/Agricultura/Ganadería III y Warden.
- Requisito End: 70 avances originales, Exploración V, Alquimia III, Pesca/Agricultura/Ganadería V, Wither y exploración del Nether.
- Registros de tandas y tipos de pociones cuando el jugador observa el proceso completo en el soporte de pociones.
- Nuevas pestañas en P para Exploración, Maestría de Herramientas y Alquimia.
- Comandos y mensajes de bloqueo adaptados.

## Pendiente antes de publicar EvolutionCraft 2.0
- Detección de pociones elaboradas aunque el jugador cierre el soporte; atribución multijugador.
- Detectar y excluir bloques colocados por el jugador para Maestría y mineral natural.
- Endurecer detección de noches sin dormir y restauración tras muerte.
- Registrar y mostrar completamente las seis misiones, incluida la misión final de Netherita.
- Aplicar restricciones de la mesa de encantamientos desde Hierro y de efectos superiores por era sin destruir objetos.
- Avances reales de Minecraft para eras, niveles y dimensión, con toasts, iconos y sonidos.
- Los 15 desafíos secretos y sus distintivos.
- Interfaz final híbrida Minecraft/RPG con jerarquía Evolución/Habilidades/Logros.
- Revisar la accesibilidad de 70 avances originales antes de entrar al End.
- Pruebas en partida y multijugador (el CI solo comprueba compilación).

**AVISO:** no tratar la rama de desarrollo como una versión estable. Puede haber requisitos todavía no verificables o bloqueos pendientes.
