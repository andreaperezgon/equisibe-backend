# Estructura y datos

## Organización por capas

| Paquete | Responsabilidad |
|---|---|
| controller | Endpoints, entrada/salida HTTP y validación de peticiones |
| dto | Contratos de peticiones y respuestas, sin exponer contraseñas |
| service | Reglas, cálculos, autorización por propietario y transacciones |
| repository | Consultas JPA y bloqueos necesarios |
| model | Entidades, valores y reglas de estado |
| security | Identidad, versión de credenciales y flujo de Google |
| config | Seguridad, CORS, servicios externos y datos de ejemplo |
| exception | Traducción de errores |

Controlador → servicio → repositorio → base de datos. Los DTO separan los objetos persistentes de la API. Las pruebas están en src/test/java y sus propiedades en src/test/resources.

## Datos principales

| Modelo | Persistencia y función |
|---|---|
| User | users: correo, contraseña codificada, rol, estado, credentialVersion y googleSubject |
| Product | products y product_size_stock: datos, precio/rebaja y existencias por talla |
| Favorite | favorites: usuario/producto, sin duplicados por pareja |
| ShippingAddress | shipping_addresses: pertenece a un usuario |
| Order | orders: propietario, estado, importes y copia de dirección |
| OrderItem | Colección de valores en order_items: copia de nombre, talla, cantidad y precio |
| OrderPayment | order_payments: sesión Stripe, idempotencia y estado de reserva |
| ReturnRequest | return_requests: pedido, identificador de petición, motivo y respuesta/estado |
| ReturnItem | Colección en return_request_items: índice de línea y cantidad |
| PasswordResetToken | password_reset_tokens: hash del token, usuario y caducidad |
| PromotionalCode | promotional_codes: código, porcentaje y habilitación |
| PromotionalPopup | promotional_popups: contenido, código asociado y activación |
| ShopSettings | shop_settings: apertura/cierre, texto y cuenta atrás |
| Contact | Registro de la consulta y datos necesarios para atenderla |

OrderItem, ReturnItem y OrderShippingAddress son valores embebidos, no entidades independientes con repositorio. productId en OrderItem es un identificador de la copia del pedido, no una relación JPA ManyToOne con Product. El vínculo de un popup con el código también se guarda como texto.

Editar un producto o dirección no altera las copias de un pedido anterior. OrderPayment se vincula uno a uno con el pedido. Campos de versión y bloqueos protegen operaciones concurrentes donde se han implementado.

Las fotos son archivos: la base conserva sus URL. La cesta se guarda en localStorage del frontend, sin tabla de cesta ni sincronización entre dispositivos. [Diagrama de datos](09-Diagramas.md).
