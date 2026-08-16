<div align="center">

  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/DrakesSlimeMarket/main/slimemarket_banner.svg" alt="DrakesSlimeMarket Banner" width="920" />

# 🏪 DrakesSlimeMarket (Tienda DrakesCraft)

**Sistema de Economía Dinámica y Comercio de Materiales Slimefun4**

<p>
  <a href="https://github.com/DrakesCraft-Labs/DrakesSlimeMarket"><img src="https://img.shields.io/badge/GitHub-DrakesSlimeMarket-181717?style=for-the-badge&logo=github" alt="GitHub"/></a>
  <img src="https://img.shields.io/badge/Java-21_FFM_Panama-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21 FFM"/>
  <img src="https://img.shields.io/badge/Rust-FFM_Accelerated-FF4500?style=for-the-badge&logo=rust&logoColor=white" alt="Rust Native"/>
  <img src="https://img.shields.io/badge/Paper-1.21.11-FFD700?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11"/>
</p>

</div>

---

## 🪙 ¿Qué es DrakesSlimeMarket?

`DrakesSlimeMarket` es el mercado curado de Slimefun para DrakesCraft. Descubre
los items registrados por el core y sus addons, pero solo publica ofertas que
superan las políticas de seguridad, complejidad y balance. Detectar un addon no
significa vender todo su contenido.

---

## 🧰 Funcionalidades Destacadas

- **Catalogo dinamico y curado**: descubre items Slimefun y materiales vanilla
  permitidos, aplica overrides y los agrupa por familias configurables.
- **Complejidad de receta**: excluye multibloques, endgame, items peligrosos o
  recetas que superan los limites de profundidad/coste configurados.
- **Rotacion por categoria**: limita ofertas visibles por familia y evita menus
  con cientos de paginas o items repetidos.
- **Precio dinamico**: combina precio base, complejidad, circulacion observada y
  compras recientes con limites minimos/maximos.
- **GUI aislada por jugador**: categorias, paginas y ofertas no comparten un
  inventario mutable entre usuarios.
- **Transaccion autoritativa**: Vault valida saldo y retiro antes de entregar;
  si el inventario no admite el item, la compra no se completa parcialmente.
- **Auditoria asincrona**: cada compra queda registrada sin bloquear el hilo
  principal y sin incluir secretos.

La web y Tebex no forman parte del runtime de este plugin. Odysseia maneja las
compras comerciales y `drakescraft-web` publica el storefront; este mercado es
exclusivamente la economia interna de materiales del servidor.

## Politica de seguridad

El catalogo falla cerrado. Las listas negras, familias permitidas, overrides y
reglas de receta tienen prioridad sobre el descubrimiento automatico. Supreme,
Infinity, armas, armaduras, recursos radiactivos, generadores endgame y otros
items de alto impacto deben permanecer excluidos salvo auditoria explicita.

Los tests verifican rotacion, politica de materiales, complejidad de recetas y
calculo de precios. El comando operativo es `/sfmercado`; `reload` y `stats`
requieren permiso administrativo.

---

## ⚡ Integración con Slimefun-Rust

El mercado consume el servicio nativo publicado por Slimefun4-Drake y delega el
cálculo determinista de precios a `libslimefun_ffi.so`. Si el ABI no está
disponible, usa automáticamente la implementación Java equivalente. Vault,
sBank y las transacciones Bukkit siempre permanecen autoritativos en Java.

---

## 🛠️ Compilación e Instalación

```bash
# Compilar paquete JAR con Maven
mvn clean package
```

Ubica el archivo compilado `DrakesSlimeMarket-1.0.jar` en la carpeta `plugins/` de tu servidor Minecraft Paper/Purpur 1.21.11.

---

<div align="center">

**DrakesCraft Labs** · Mantenido por [**JackStar6677-1**](https://github.com/JackStar6677-1)

</div>
