# API del backend

Revisión del **28 de octubre de 2026** sobre el código local de las ramas de desarrollo. No se afirma que los cambios estén integrados en main o publicados en GitHub.

Base local: http://localhost:8080. JSON para peticiones habituales y multipart para subir fotos. Todas las escrituras requieren CSRF salvo el webhook firmado de Stripe. El cierre de tienda puede impedir operaciones nuevas aunque exista permiso.

## Rutas extraídas de los controladores

| Método | Ruta | Acceso | Controlador |
|---|---|---|---|
| GET | /api/admin/orders | ADMIN | AdminOrderController |
| GET | /api/admin/orders/{orderId} | ADMIN | AdminOrderController |
| PUT | /api/admin/orders/{orderId}/status | ADMIN | AdminOrderController |
| GET | /api/admin/products | ADMIN | AdminProductController |
| GET | /api/admin/products/{productId} | ADMIN | AdminProductController |
| POST | /api/admin/products | ADMIN | AdminProductController |
| PUT | /api/admin/products/{productId} | ADMIN | AdminProductController |
| PUT | /api/admin/products/{productId}/active | ADMIN | AdminProductController |
| PUT | /api/admin/products/{productId}/stock | ADMIN | AdminProductController |
| PUT | /api/admin/products/{productId}/sale | ADMIN | AdminProductController |
| GET | /api/admin/returns | ADMIN | AdminReturnController |
| PUT | /api/admin/returns/{id}/status | ADMIN | AdminReturnController |
| GET | /api/auth/csrf | Público con CSRF en escrituras | AuthController |
| POST | /api/auth/register | Público con CSRF en escrituras | AuthController |
| POST | /api/auth/login | Público con CSRF en escrituras | AuthController |
| GET | /api/auth/me | Sesión y propiedad cuando corresponda | AuthController |
| POST | /api/cart/validate | Sesión y propiedad cuando corresponda | CartController |
| POST | /api/checkout/validate | Sesión y propiedad cuando corresponda | CheckoutController |
| POST | /api/contacts | Público con CSRF en escrituras | ContactController |
| GET | /api/favorites | Sesión y propiedad cuando corresponda | FavoriteController |
| PUT | /api/favorites/{productId} | Sesión y propiedad cuando corresponda | FavoriteController |
| DELETE | /api/favorites/{productId} | Sesión y propiedad cuando corresponda | FavoriteController |
| GET | /api/orders | Sesión y propiedad cuando corresponda | OrderController |
| POST | /api/orders | Sesión y propiedad cuando corresponda | OrderController |
| GET | /api/orders/{orderId} | Sesión y propiedad cuando corresponda | OrderController |
| POST | /api/auth/forgot-password | Público con CSRF en escrituras | PasswordResetController |
| POST | /api/auth/reset-password | Público con CSRF en escrituras | PasswordResetController |
| GET | /api/popups/active | Público con CSRF en escrituras | PopupController |
| GET | /api/admin/popups | ADMIN | PopupController |
| POST | /api/admin/popups | ADMIN | PopupController |
| PUT | /api/admin/popups/{id} | ADMIN | PopupController |
| PUT | /api/admin/popups/{id}/active | ADMIN | PopupController |
| DELETE | /api/admin/popups/{id} | ADMIN | PopupController |
| POST | /api/admin/popups/examples | ADMIN | PopupController |
| GET | /api/products | Público con CSRF en escrituras | ProductController |
| GET | /api/products/{id} | Público con CSRF en escrituras | ProductController |
| POST | /api/admin/product-images | ADMIN | ProductImageController |
| GET | /api/product-images/{filename} | Público con CSRF en escrituras | ProductImageController |
| GET | /api/orders/{orderId}/returns | Sesión y propiedad cuando corresponda | ReturnRequestController |
| POST | /api/orders/{orderId}/returns | Sesión y propiedad cuando corresponda | ReturnRequestController |
| GET | /api/addresses | Sesión y propiedad cuando corresponda | ShippingAddressController |
| GET | /api/addresses/{id} | Sesión y propiedad cuando corresponda | ShippingAddressController |
| POST | /api/addresses | Sesión y propiedad cuando corresponda | ShippingAddressController |
| PUT | /api/addresses/{id} | Sesión y propiedad cuando corresponda | ShippingAddressController |
| GET | /api/shop/status | Público con CSRF en escrituras | ShopSettingsController |
| PUT | /api/admin/shop/settings | ADMIN | ShopSettingsController |
| POST | /api/orders/{orderId}/checkout-session | Sesión y propiedad cuando corresponda | StripeCheckoutController |
| POST | /api/payments/stripe/webhook | Firma Stripe | StripeWebhookController |

## Rutas de Spring Security

| Método | Ruta | Función |
|---|---|---|
| POST | /api/auth/logout | Cierra sesión y elimina JSESSIONID; exige CSRF |
| GET | /oauth2/authorization/google | Inicia OAuth con el perfil google configurado |
| GET | /login/oauth2/code/google | Callback exacto autorizado en Google |

/api/admin/e2e/orders es un endpoint de fixtures exclusivamente del código de pruebas, no una API de producción.

## Sesión y CSRF

1. GET /api/auth/csrf manteniendo las cookies.
2. Enviar la cabecera indicada por la respuesta junto al JSON de escritura.
3. POST /api/auth/login con email y password; conservar la cookie.
4. Obtener un CSRF actualizado tras iniciar sesión.
5. GET /api/auth/me consulta la cuenta. Logout responde 204.

No enviar identificadores de otro usuario esperando que el servidor los acepte: cada servicio comprueba la propiedad.

## Ejemplos de cuerpos

### Compra y creación de pedido

```json
{
  "shippingAddressId": 1,
  "items": [{"productId": 1, "size": "S", "quantity": 1}],
  "promoCode": "EQUISIBE10"
}
```

POST /api/checkout/validate calcula sin crear/cobrar. POST /api/orders crea el pedido pendiente y devuelve 201 con id y Location. GET /api/orders/{orderId} consulta su estado. La cesta tiene entre 1 y 100 líneas y promoCode admite hasta 40 caracteres. Los identificadores del ejemplo deben sustituirse por datos de la cuenta.

POST /api/orders/{orderId}/checkout-session abre el circuito de Stripe del pedido del propietario. La firma del webhook, no el retorno del navegador, permite confirmar el pago.

### Solicitud de devolución

```json
{
  "clientRequestId": "11111111-1111-4111-8111-111111111111",
  "type": "WITHDRAWAL",
  "note": "Solicito la devolución de esta unidad.",
  "items": [{"lineIndex": 0, "quantity": 1}]
}
```

El UUID representa una solicitud lógica: generar uno nuevo para una solicitud distinta y conservarlo en un reintento de la misma. Reutilizarlo con otros datos causa conflicto. lineIndex empieza en cero sobre las líneas del pedido; quantity debe ser positiva y no superar las unidades aún disponibles. Solo se admite WITHDRAWAL y pedidos con pago confirmado; las cantidades de solicitudes no rechazadas se tienen en cuenta. note admite hasta 2000 caracteres.

### Administración

```json
{"status": "PREPARING"}
```

Para PUT /api/admin/orders/{orderId}/status. Deben seguirse las transiciones del modelo.

```json
{"status": "CLOSED", "note": "Solicitud resuelta con el cliente."}
```

Para PUT /api/admin/returns/{id}/status. CLOSED y REJECTED exigen explicación y solo se aceptan desde el estado permitido.

```json
{"title": "Promoción del taller", "text": "Consulta nuestra promoción.", "promoCode": "EQUISIBE10"}
```

Para crear/editar un popup. title obligatorio hasta 120 caracteres; text obligatorio hasta 2000; promoCode opcional hasta 40. La activación se gestiona por su endpoint específico.

Las DTO de producto recogen name, description, price, imageUrl y category. Stock y rebaja tienen endpoints separados. Para subir una imagen usar /api/admin/product-images con el campo file, no un JSON de bytes.

### Recuperación de contraseña

```json
{"email": "cliente@example.test"}
```

POST /api/auth/forgot-password da una respuesta genérica. POST /api/auth/reset-password utiliza token y password: token de 43 caracteres base64url y contraseña entre 8 y 64 caracteres.

## Errores y reglas

400: datos inválidos o promoción no disponible; 401: sesión necesaria; 403: permiso/CSRF; 404: recurso inexistente/no accesible según operación; 409: conflicto de cantidad, versión o estado; 503: tienda cerrada o servicio no disponible según operación. No depender únicamente del texto del mensaje.

Los DTO y validaciones de src/main/java/com/equisibe/dto y los records de los controladores son la referencia detallada de campos. Esta página enumera las rutas implementadas, no afirma disponer de un servidor OpenAPI/Swagger.
