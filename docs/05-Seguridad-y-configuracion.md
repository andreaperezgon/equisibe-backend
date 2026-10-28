# Seguridad y configuración

## Identidad y permisos

Sesiones HTTP con JSESSIONID y contraseñas BCrypt mediante PasswordEncoder. No se utiliza JWT. El login cambia la sesión cuando corresponde, guarda el contexto y renueva CSRF; logout invalida sesión y cookie. CredentialVersionFilter comprueba la vigencia de credenciales, también después de un cambio de contraseña.

Las rutas /api/admin/** requieren ADMIN. La cuenta autenticada solo puede consultar/modificar sus pedidos, direcciones, favoritos y devoluciones. Ocultar un botón no concede ni sustituye permisos.

Las operaciones de escritura requieren CSRF, incluso en formularios públicos. El webhook de Stripe se excluye de CSRF porque valida la firma Stripe-Signature con su secreto. CORS está configurado para http://localhost:5173 con credenciales; revisar origen/cookies antes de publicar.

## Google y recuperación

Google utiliza OAuth2/OIDC con scopes openid,email,profile. El perfil google configura cliente y callback. GoogleAccountService resuelve la identidad del proveedor y no vincula automáticamente un correo local ya existente. GoogleLoginHandler crea la sesión local y actualmente redirige a localhost:5173/account.

La recuperación genera un token aleatorio, guarda su hash y caduca a los 30 minutos. El cambio valida el token, lo consume y cambia la versión de credenciales. La respuesta al solicitar recuperación evita revelar si existe el correo. El servicio limita solicitudes repetidas dentro del proceso; esto no se presenta como protección distribuida de producción.

## Variables y propiedades

| Nombre | Uso |
|---|---|
| MAIL_USERNAME / MAIL_PASSWORD | Cuenta SMTP y contraseña de aplicación |
| APP_MAIL_ENABLED | Habilita envío de correo; false por defecto |
| CONTACT_RECIPIENT | Destino de Contacto; por defecto equisibe@gmail.com |
| PASSWORD_RESET_BASE_URL | URL del formulario con token; por defecto localhost:5173/reset-password |
| GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET | Cliente OAuth, solo backend |
| GOOGLE_REDIRECT_URI | Callback, por defecto localhost:8080/login/oauth2/code/google |
| STRIPE_SECRET_KEY | Clave de pruebas sk_test_ |
| STRIPE_WEBHOOK_SECRET | Verificación de los eventos |
| STOCK_RECONCILIATION_ENABLED | Habilita reconciliación programada fuera de test |
| app.frontend-url | Retorno de Stripe; por defecto localhost:5173 |
| app.product-images-directory | Carpeta de subida; por defecto uploads/products |
| app.seed-products | Activa catálogo de ejemplo cuando está vacío |
| spring.datasource.* | Conexión con la base |
| spring.jpa.hibernate.ddl-auto | update en desarrollo; create-drop en pruebas |
| spring.servlet.multipart.max-file-size | 10 MB |
| spring.servlet.multipart.max-request-size | 11 MB |

Spring permite equivalentes de entorno para sus propiedades, como APP_FRONTEND_URL. Google requiere el perfil google además de sus variables. Las URL locales no están totalmente centralizadas: cambiar solo una variable no prepara el despliegue.

SMTP utiliza smtp.gmail.com:587, autenticación y STARTTLS, con tiempos de espera. No enviar la contraseña normal de Google al frontend.

## Imágenes

Subida solo ADMIN. JPG/PNG de hasta 10 MB y 16 megapíxeles, validando el contenido real y dimensiones. Se guarda un archivo con nombre aleatorio; GET de la imagen es público. Las fotos necesitan copia de seguridad y persistencia separadas. No hay limpieza automática completa de archivos antiguos/huérfanos.

## Pago y negocio

Stripe está restringido al modo prueba; se rechazan claves/eventos de modo real. El servidor calcula los importes, reserva existencias y verifica importe, moneda, sesión y pedido al confirmar. Los reintentos deben ser idempotentes para no confirmar/liberar dos veces.

Un código solo aplica si está habilitado y hay un popup activo con el mismo código. Las rebajas propias de cada producto son un mecanismo independiente.

Nunca publicar .env.local, claves, contraseñas, cookies, tokens ni datos de clientes.
