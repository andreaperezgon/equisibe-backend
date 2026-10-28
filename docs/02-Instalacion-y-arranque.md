# Instalación y arranque

## Requisitos

Git, Java 21, Maven y Docker/Compose para MySQL. Stripe CLI solo es necesario para el circuito de pagos de prueba. Entrar en la carpeta que contiene pom.xml; en la instalación actual:

```bash
cd "/c/Users/usuario/Documents/EQUISIBÉ/equisibe-backend/equisibe-backend"
java -version
mvn -version
docker compose up -d mysql
```

Compose utiliza MySQL 8.4, puerto 3306, base equisibe y volumen equisibe_mysql_data. Las credenciales incluidas son de desarrollo. No usar docker compose down -v si se quieren conservar los datos.

## Variables de la terminal

Spring no importa automáticamente .env.local en esta configuración. Si el archivo local ya contiene asignaciones válidas de shell y se ha revisado, puede cargarse en Git Bash:

```bash
set -a
source .env.local
set +a
```

Solo cargar archivos propios de confianza: source ejecuta su contenido. Otra opción es exportar las variables individualmente. No publicar el archivo ni sus valores. En [Seguridad y configuración](05-Seguridad-y-configuracion.md) se enumeran los nombres.

## Arranque habitual

```bash
mvn spring-boot:run
```

Con las credenciales OAuth preparadas, para habilitar Google:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=google
```

Esperar a Started EquisibeBackendApplication. La API escucha en http://localhost:8080. Ctrl+C detiene el proceso. El frontend se arranca por separado en localhost:5173. El wrapper mvnw/mvnw.cmd también está disponible.

## Stripe de prueba

En otra terminal:

```bash
stripe login
stripe listen --events checkout.session.completed,checkout.session.async_payment_succeeded,checkout.session.expired --forward-to localhost:8080/api/payments/stripe/webhook
```

Configurar STRIPE_SECRET_KEY con una clave sk_test_ y STRIPE_WEBHOOK_SECRET con el whsec_ del listener. Son valores distintos. Si cambia el secreto del listener, actualizarlo y reiniciar el backend.

Sin acceso a Stripe, la navegación puede funcionar pero el pago/reconciliación puede informar que no está disponible. Las pruebas automáticas test/e2e no necesitan un listener ni claves reales.

## Compilar y probar

```bash
mvn -DskipTests compile
mvn test
mvn -Pcoverage clean verify
```

El último comando ejecuta pruebas y genera target/site/jacoco/index.html. clean elimina resultados anteriores; esperar a que termine un proceso antes de iniciar otro sobre el mismo target.
