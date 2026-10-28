# Equisibé · Backend

API del taller y tienda: sesiones, Google, correo, catálogo, compra de prueba, pedidos, devoluciones y administración.

**Java 21 · Spring Boot 4.1.1 · Spring Security · JPA · MySQL 8.4**

## Documentación

[Índice completo](docs/Home.md) · [Instalación](docs/02-Instalacion-y-arranque.md) · [API](docs/04-API.md) · [Seguridad](docs/05-Seguridad-y-configuracion.md) · [Pruebas](docs/06-Pruebas.md) · [Diagramas Mermaid](docs/09-Diagramas.md)

[Wiki del backend](https://github.com/andreaperezgon/equisibe-backend/wiki) · [Frontend](https://github.com/andreaperezgon/equisibe-frontend)

## Arranque

Con Java 21, Maven, Docker y variables preparadas según la guía, desde la carpeta con pom.xml:

```bash
docker compose up -d mysql
mvn spring-boot:run
```

API: http://localhost:8080. Para habilitar Google, configurar sus variables y activar el perfil google.

```bash
mvn test
mvn -Pcoverage clean verify
```

Informe: target/site/jacoco/index.html.

## Calidad documentada: 28/10/2026

- **411/411 pruebas, 100 % pasadas**: 0 fallos, 0 errores y 0 omitidas.
- JaCoCo: **96,41 % líneas, 86,93 % ramas, 95,93 % instrucciones, 95,13 % métodos y 100 % clases**.
- **22/22 escenarios E2E verificados entre ejecuciones sucesivas**, con H2 aislada; no equivalen a pagos o servicios externos probados en vivo.

[Detalle y comandos](docs/06-Pruebas.md). Porcentaje de pruebas correctas y cobertura son métricas distintas.

La entrega funcional local se da por terminada; el despliegue público es una fase posterior. Stripe está limitado a modo prueba. La actualización de docs no implica commit, push o merge a main.
