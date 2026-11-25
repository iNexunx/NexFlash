# Guía de diseño y propuesta de icono

Guía práctica, limpia y funcional para NexFlash y aplicable a cualquier app Android moderna.

## 1. Guía de diseño de interfaz

### 1.1 Estructura visual

| Elemento | Regla | Especificación |
|---|---|---|
| Bordes redondeados | Jerarquía consistente | Contenedores 24–28dp; botones/píldoras 999dp; campos 20–24dp; chips 12–16dp. No mezclar radios arbitrarios en la misma pantalla. |
| Contenedores | Limpios, con margen lateral | Margen horizontal 16dp (compacto 12dp). Padding interno 16–20dp. Un contenedor = un propósito. Usar `surfaceContainer*`; evitar bordes decorativos pesados. |
| Filas/botones píldora | Contorno definido, icono izquierda, texto claro | Altura mínima 48dp. Icono 18–20dp izquierda, gap 8–12dp, texto en una línea cuando sea posible; elipsis solo si es inevitable. Contorno 1dp con `outline`/`outlineVariant`; filled/tonal según jerarquía. |
| Switches integrados | Fila completa, alineación clara | Label izquierda, switch derecha; descripción breve debajo si aplica. La fila puede ser clicable completa. Nunca dejar switch sin contexto. |
| Espaciado | Ritmo predecible | Entre secciones 16–24dp; entre ítems 8–12dp; entre campo y botón 12–16dp. Sin encimamientos. |

### 1.2 Coherencia de tema

| Área | Comportamiento |
|---|---|
| Modos | `Claro`, `Oscuro`, `Seguir sistema` (predeterminado). Acento: `Dinámico/Predeterminado` primero; acentos manuales secundarios. |
| Fuente de verdad | Un solo estado para tema/acento/idioma. Aplicar antes del primer frame visible; evitar recreates innecesarios. |
| Consistencia | Mismos tokens de color, forma, tipografía y elevación en pantallas, menús, ajustes, diálogos, bottom sheets, toasts/snackbars y notificaciones. |
| Sin parpadeos | No alternar entre paleta default y dinámica durante carga. Persistir preferencia y resolver tema en la raíz. |
| Notificaciones | Icono monocromo; color/acento coherente con marca o sistema; tono funcional consistente (éxito/error/neutro). |
| Predeterminado | Fiel al sistema/identidad base: dynamic color cuando esté disponible; si no, seed de marca sobrio. |

### 1.3 Comportamiento de controles

- **Botones**: acento por rol. Primario = acción principal; tonal/outlined = secundaria; text = terciaria/destructiva contextual. Estados disabled/focus/pressed claros, sin saltos de layout.
- **Switches**: sin rebote visual ni estados cruzados. La UI refleja el estado real persistido/confirmado; en async, usar loading/disabled temporal y rollback claro ante error.
- **Menús/paneles**: misma forma, padding, tipografía e iconografía. Ítems destructivos separados y etiquetados; sin duplicados.
- **Diálogos**: padding uniforme (24dp), título claro, cuerpo corto, acciones abajo con separación 8–12dp. Campos nunca pegados a botones; validación inline; no cerrar por toque fuera durante operaciones críticas.
- **Multiidioma**: strings externalizados, sin hardcode. Permitir expansión 30–40%; evitar anchos fijos por texto. Formatos/plurales localizados. RTL con `start/end`.

### 1.4 Barras del sistema, teclado y modularidad

- **Insets**: respetar `systemBars`, `displayCutout` e `ime`. Usar `Scaffold`/`WindowInsets.safeDrawing`; nunca colocar contenido interactivo bajo status/nav/teclado.
- **Teclado**: input enfocado visible con IME; `adjustResize`/insets IME; scroll al campo activo. Sin huecos al cerrar teclado.
- **Safe margins**: targets táctiles mínimos 48x48dp; respetar bordes gestuales.
- **Modularidad**: vistas compactas/mini-paneles opcionales; accesos rápidos en píldoras; controles flotantes solo si no tapan contenido/navegación, con posición predecible y estado persistente.

### 1.5 Checklist

- [ ] Tokens definidos: color, radius, spacing, typography, elevation.
- [ ] Tema por defecto = seguir sistema/dynamic cuando aplique.
- [ ] Sin strings/colores hardcodeados fuera de tokens.
- [ ] Diálogos/menús/snackbars usan mismo `ColorScheme` y shapes.
- [ ] Probado: claro/oscuro, dynamic on/off, idioma largo, RTL, pantalla chica, IME abierto, rotación.

## 2. Propuesta de icono principal

### 2.1 Concepto

- **Metáfora**: rayo geométrico centrado = flash/velocidad/acción.
- **Estilo**: limpio, geométrico, humano; planos uniformes; cero estética genérica de IA.
- **Evitar**: neón, glow exagerado, 3D plástico, sombras difusas, texturas, ornamentos innecesarios.

### 2.2 Construcción

| Capa | Especificación |
|---|---|
| Fondo | Sólido o degradado sutil muy controlado (misma familia tonal, 5–8% diferencia máx.). NexFlash: base `#6750A4` con forma tonal sutil. |
| Forma principal | Rayo simple, centrado ópticamente en safe area adaptive. Bordes rectos, ángulos definidos, proporciones estables. |
| Color forma | Blanco alto contraste (`#FFFFFF`) con plano secundario opcional `#EADDFF` a baja opacidad (<20%) para volumen geométrico, no sombra. |
| Monocromo | Silueta plana negra para Android 13 themed icons; sin semitonos frágiles. |
| Grid | Viewport 108x108; contenido clave dentro de safe zone ~66dp; margen respirable. |

### 2.3 Tamaños y adaptación

| Contexto | Ajuste |
|---|---|
| Launcher grande | Forma completa; detalle secundario solo si se lee limpio. |
| Launcher pequeño/carpeta | Priorizar silueta del rayo; reducir detalles secundarios. |
| Notificación/status | Asset monocromo plano, sin fondo/degradado/opacidades finas. |
| Muy pequeño/favicon | Rayo simplificado, más grueso visualmente, sin cortes internos. |

### 2.4 Contraste claro/oscuro

| Modo | Fondo | Icono | Contraste |
|---|---|---|---|
| Claro/dynamic claro | Seed claro o contenedor de marca | Rayo oscuro/primary alto contraste | ≥ 4.5:1 |
| Oscuro/dynamic oscuro | Sólido oscuro sutil | Rayo blanco/tonal claro | ≥ 4.5:1 |
| Android 13 themed | Sistema | Monocromo plano `ic_launcher_monochrome` | Silueta clara |

### 2.5 Validación

- [ ] Reconocible a 24dp en blanco y negro.
- [ ] Centrado óptico.
- [ ] Sin texto.
- [ ] Funciona sobre wallpapers claros/oscuros/coloridos.
- [ ] Assets: adaptive foreground/background/monochrome, xml round/default.
