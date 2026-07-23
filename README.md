<div align="center">

  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/DrakesSlimeMarket/main/slimemarket_banner.svg" alt="DrakesSlimeMarket Banner" width="920" />

# 🏪 DrakesSlimeMarket (Tienda DrakesCraft)

**Sistema de Economía Dinámica, Comercio de Materiales Slimefun4 y Aceleración Nativa en Rust**

<p>
  <a href="https://github.com/DrakesCraft-Labs/DrakesSlimeMarket"><img src="https://img.shields.io/badge/GitHub-DrakesSlimeMarket-181717?style=for-the-badge&logo=github" alt="GitHub"/></a>
  <img src="https://img.shields.io/badge/Java-21_FFM_Panama-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21 FFM"/>
  <img src="https://img.shields.io/badge/Rust-FFM_Accelerated-FF4500?style=for-the-badge&logo=rust&logoColor=white" alt="Rust Native"/>
  <img src="https://img.shields.io/badge/Paper-1.21.11-FFD700?style=for-the-badge&logo=minecraft&logoColor=white" alt="Paper 1.21.11"/>
</p>

</div>

---

## 🪙 ¿Qué es DrakesSlimeMarket?

`DrakesSlimeMarket` es el plugin de tienda y mercado dinámico para DrakesCraft. Indexa automáticamente todos los materiales, lingotes, polvos y productos de Slimefun4 y sus 44 Addons para ofrecer compra/venta en tiempo real con fluctuación de precios según la oferta y demanda del servidor.

---

## 🧰 Funcionalidades Destacadas

- 📈 **Mercado Dinámico de Slimefun**: Ajusta los precios de compra y venta según la cantidad de materiales vendidos por los jugadores.
- 🏪 **Integración con Tienda Web & Tebex**: Conexión bidireccional entre la tienda del juego (`/tienda` o `/sfmarket`) y el portal web de DrakesCraft.
- 🛡️ **Prevención de Inflación & Dupes**: Auditoría en tiempo real de transacciones para evitar compras masivas por desincronización de lag.

---

## ⚡ Aceleración Nativa en Rust (Modelo Híbrido Cero-Riesgo)

`DrakesSlimeMarket` incluye el puente Panama FFM **`RustNativeBridge`** para delegar la indexación del catálogo de precios de los 44 Addons al motor nativo `Slimefun-Rust` (`slimefun_ffi`):
- 🚀 **Indexación de Precios en Nanosegundos**: Sin sobrecarga de CPU ni pausas de Garbage Collector.
- 🛡️ **Preservación Total sin Reset (SQLite 0-Reset)**: Interfaz 1:1 con la base de datos `stored-blocks.db`.

---

## 🛠️ Compilación e Instalación

```bash
# Compilar paquete JAR con Maven
mvn clean package
```

Ubica el archivo compilado `DrakesSlimeMarket-1.0.jar` en la carpeta `plugins/` de tu servidor Minecraft Paper/Purpur 1.21.11.

---

<div align="center">

**DrakesCraft Labs** · Maintained by [**JackStar6677-1**](https://github.com/JackStar6677-1)

</div>