# Pruebas y cobertura del backend

El porcentaje de pruebas pasadas compara casos correctos con casos ejecutados. La cobertura mide qué código se ha recorrido. Un 100 % de pruebas pasadas no significa un 100 % de cobertura ni garantiza ausencia de errores. Las ejecuciones dirigidas se solapan con la suite completa y no deben sumarse como si fueran casos nuevos.

## Resultado completo del 28/10/2026

**411 pruebas ejecutadas, 411 correctas: 100 % pasadas.** Los XML TEST-*.xml de target/surefire-reports suman 0 fallos, 0 errores y 0 omitidas.

| Métrica JaCoCo | Cubierto / total | Porcentaje |
|---|---:|---:|
| Líneas | 2040 / 2116 | **96,41 %** |
| Ramas | 605 / 696 | **86,93 %** |
| Instrucciones | 8444 / 8802 | **95,93 %** |
| Métodos | 508 / 534 | **95,13 %** |
| Clases | 122 / 122 | **100 %** |

Origen: target/site/jacoco/jacoco.xml, informe completo guardado el 28/10/2026. El 100 % de clases significa que se ejecutó código de cada clase; no que todas sus líneas o decisiones estén cubiertas. No se han eliminado clases del informe para elevar estas cifras.

## Tipos y alcance

- Unitarias: cálculos de precios, descuentos/envío, modelos, cantidades, transiciones, credenciales y servicios.
- Integración: API, sesión/CSRF/roles, propiedad de recursos y persistencia H2.
- Escenarios de fallo: Stripe/webhooks, reservas, correo, imágenes, datos inválidos, idempotencia y concurrencia donde corresponda.
- Promociones: activación/desactivación/borrado, varios popups del mismo código, códigos ajenos y códigos deshabilitados.
- Google y recuperación: resolución de identidad, rechazo de vinculaciones no autorizadas, tokens y vigencia de credenciales.

JUnit, Mockito y MockMvc ejecutan las pruebas; H2 utiliza create-drop y Docker está desactivado en test. Las llamadas externas se sustituyen o controlan en las pruebas correspondientes. No se declara una validación en vivo de todos los servicios externos.

## Comandos

Desde la carpeta con pom.xml:

```bash
mvn test
mvn -Pcoverage clean verify
```

Para ejecutar pruebas con cobertura sin limpiar:

```bash
mvn -Pcoverage test jacoco:report
```

Para una repetición dirigida:

```bash
mvn -Pcoverage "-Dtest=PromotionalDiscountServiceTest,PopupDiscountIntegrationTest" test
```

Una selección -Dtest no mide toda la suite. No atribuir su informe parcial al conjunto del backend. Evitar mezclar datos de clases compiladas diferentes; una medición final reproducible usa clean verify.

## Informes y cierre de la JVM

Abrir target/site/jacoco/index.html. jacoco.exec es un archivo binario generado, no se edita; borrarlo obliga a volver a ejecutar pruebas para regenerarlo. target/surefire-reports contiene los resultados JUnit.

En ejecuciones anteriores, Surefire esperaba el cierre de la JVM y la terminaba antes de que JaCoCo guardase datos. Un BUILD SUCCESS no garantizaba entonces cobertura válida. Se configuró el agente, margen de salida y comunicación de Surefire, y se redujo el SQL de pruebas. El informe final indicado sí tiene datos válidos.

No usar forkCount=0 para esquivar el problema: el agente está conectado al proceso de pruebas. No ejecutar dos Maven a la vez sobre target ni borrar sus archivos mientras trabajan.


## E2E con Playwright

Se han validado **22/22 escenarios de Chromium (100 %) entre ejecuciones sucesivas**, no en una única ejecución final. La ejecución inicial obtuvo 18/21; se corrigieron el selector del popup y la pérdida de cesta al recargar, y se añadió la separación de cesta por cuenta. Los cuatro escenarios afectados terminaron pasando en repeticiones dirigidas.

Los errores de suspensión de red y de arranque de Chromium no se contaron como éxitos. El checkout superó el límite inicial de un minuto y pasó al repetirlo con 120 segundos.

| Área | Casos | Alcance |
|---|---:|---|
| Navegación | 6 | Inicio, privacidad/devoluciones desde footer, login/registro y rutas protegidas |
| Cliente | 10 | Registro, sesión, errores de acceso, direcciones, favoritos, tallas, cesta, checkout, permisos y cesta separada por cuenta |
| Administración | 6 | Productos, imágenes, rebajas/stock, pedidos, devoluciones, descuento por popup y dos tipos de cierre |
| Total | **22** | Frontend y backend conectados con datos ficticios |

Las E2E no realizan cobros, envíos reales de correo ni el recorrido externo de Google. Los pedidos pagados de administración son fixtures, no evidencia de un pago real. Las integraciones externas tienen pruebas de backend con dobles de prueba.

### Arranque del entorno aislado

Detener primero el backend habitual del puerto 8080. En Git Bash:

```bash
cd "/c/Users/usuario/Documents/EQUISIBÉ/equisibe-backend/equisibe-backend"
mvn spring-boot:test-run -Dspring-boot.run.profiles=test,e2e
```

Esperar a Started EquisibeBackendApplication. En otra terminal:

```bash
cd "/c/Users/usuario/Documents/EQUISIBÉ/equisibe-frontend/equisibe-frontend"
npx playwright install chromium
npm run test:e2e -- --timeout=120000
```

Playwright prepara Vite en localhost:5173 o reutiliza el servidor local. Mantener el equipo despierto. Para abrir su interfaz o informe:

```bash
npm run test:e2e:ui
npm run test:e2e:report
```

El perfil test,e2e usa jdbc:h2:mem:equisibe_e2e, sin Docker, correo ni reconciliación de Stripe. Las imágenes temporales van a target/e2e-uploads. Las fixtures y /api/admin/e2e/orders están en src/test, fuera del JAR de producción; el endpoint exige ADMIN y CSRF. El helper verifica X-Equisibe-E2E: isolated-memory antes de modificar datos.

Cuentas exclusivamente ficticias: e2e-admin@example.test y e2e-customer@example.test, contraseña E2eOnly!2026. No son cuentas reales del negocio. El resto se genera con identificadores únicos. Al terminar, Ctrl+C en el backend temporal y arrancar el habitual. Stripe CLI no es necesario.
