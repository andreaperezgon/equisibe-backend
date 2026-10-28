# Diagramas del backend

## Arquitectura por capas

```mermaid
flowchart LR
    Persona[Cliente o administrador] --> Vue[SPA Vue 3]
    Vue --> Router[Vue Router]
    Vue --> Pinia[Estado Pinia]
    Pinia --> Local[localStorage: cesta por cuenta]
    Vue --> Axios[Servicios Axios]
    Axios -->|HTTP y cookie| Security[Spring Security y CSRF]
    Security --> Controller[Controladores REST]
    Controller --> Service[Servicios de negocio]
    Service --> Repo[Repositorios JPA]
    Repo --> MySQL[(MySQL en desarrollo)]
    Repo -. pruebas .-> H2[(H2 en memoria)]
    Service --> Stripe[Stripe en modo prueba]
    Service --> Mail[Gmail SMTP]
    Security --> Google[Google OAuth2 y OIDC]
    Controller --> Images[Disco: fotos de productos]
```

El backend es una aplicación organizada por capas, no microservicios. El frontend es una SPA con componentes, vistas, servicios y stores. El servidor aplica permisos y reglas de negocio aunque el navegador ya compruebe el acceso.


## Modelo de datos principal

Diagrama lógico simplificado. Se distinguen las entidades de las colecciones de valores. No se dibuja una clave foránea inexistente desde el producto actual a la copia de una línea de pedido.

```mermaid
erDiagram
    USERS ||--o{ SHIPPING_ADDRESSES : tiene
    USERS ||--o{ FAVORITES : guarda
    PRODUCTS ||--o{ FAVORITES : aparece
    USERS ||--o{ ORDERS : realiza
    USERS ||--o{ PASSWORD_RESET_TOKENS : solicita
    PRODUCTS ||--o{ PRODUCT_SIZE_STOCK : contiene
    ORDERS ||--|{ ORDER_ITEMS : copia_lineas
    ORDERS ||--o| ORDER_PAYMENTS : tiene
    ORDERS ||--o{ RETURN_REQUESTS : recibe
    RETURN_REQUESTS ||--|{ RETURN_REQUEST_ITEMS : selecciona
    USERS {
        Long id PK
        String email
        String password
        String role
        String google_subject
        long credential_version
    }
    PRODUCTS {
        Long id PK
        String name
        Decimal price
        Decimal sale_price
        String image_url
        boolean active
        long stock_version
    }
    PRODUCT_SIZE_STOCK {
        Long product_id FK
        String size
        int quantity
    }
    SHIPPING_ADDRESSES {
        Long id PK
        Long user_id FK
        String recipient_name
        String address_line1
        String postal_code
    }
    FAVORITES {
        Long id PK
        Long user_id FK
        Long product_id FK
    }
    ORDERS {
        Long id PK
        Long user_id FK
        String status
        Decimal subtotal
        Decimal shipping_cost
        Decimal total
        String currency
    }
    ORDER_ITEMS {
        Long order_id FK
        int line_position
        Long product_id
        String product_name
        String size
        long quantity
        Decimal unit_price
    }
    ORDER_PAYMENTS {
        Long id PK
        Long order_id FK
        String idempotency_key
        String stripe_session_id
        String stock_reservation_status
    }
    RETURN_REQUESTS {
        Long id PK
        Long order_id FK
        UUID client_request_id
        String type
        String status
        String note
        String admin_note
    }
    RETURN_REQUEST_ITEMS {
        Long return_request_id FK
        int line_index
        long quantity
    }
    PASSWORD_RESET_TOKENS {
        Long id PK
        Long user_id FK
        String token_hash
        Instant expires_at
    }
```

El atributo password guarda la contraseña codificada. product_id en ORDER_ITEMS es un valor escalar de la copia del pedido, no una FK JPA hacia PRODUCTS. OrderShippingAddress está embebida en orders, sin tabla propia. PRODUCT_SIZE_STOCK, ORDER_ITEMS y RETURN_REQUEST_ITEMS son colecciones de valores. Contact, ShopSettings, PromotionalCode y PromotionalPopup son modelos adicionales sin relación de propietario representada en este diagrama. PromotionalPopup guarda el código como texto, no como FK JPA.

## Sesión y CSRF

```mermaid
sequenceDiagram
    actor Cliente
    participant Vue
    participant API as Spring Security y API
    participant DB as Base de datos
    Vue->>API: GET csrf
    API-->>Vue: Token y cookie
    Cliente->>Vue: Email y contraseña
    Vue->>API: POST login con CSRF
    API->>DB: Consulta cuenta
    API->>API: Comprueba BCrypt y renueva sesión
    API-->>Vue: Sesión autenticada
    Vue->>API: GET me y nuevo CSRF
    API-->>Vue: Cuenta y rol
    Vue->>Vue: Asigna propietario de la cesta
```

## Compra y confirmación de pago

```mermaid
sequenceDiagram
    actor Cliente
    participant Vue
    participant API
    participant DB
    participant Stripe
    Cliente->>Vue: Dirección, prendas y código
    Vue->>API: Validar checkout
    API->>DB: Comprueba propiedad, stock y promoción activa
    API-->>Vue: Importes recalculados
    Note over API,DB: Validar no crea ni cobra pedido
    Cliente->>Vue: Continuar al pago
    Vue->>API: Crear pedido
    API->>DB: Guarda pedido pendiente y copias
    Vue->>API: Solicita sesión de checkout
    API->>DB: Gestiona reserva de existencias e idempotencia
    API->>Stripe: Crea o recupera sesión de prueba
    Stripe-->>API: URL y datos de sesión
    API-->>Vue: URL de checkout
    Vue->>Stripe: Abre checkout
    Stripe->>API: Webhook firmado
    API->>API: Verifica firma, modo, importe, moneda y pedido
    API->>DB: Confirma pago/reserva o libera al caducar
    Vue->>API: Consulta pedido
    API-->>Vue: Estado confirmado por servidor
```

La creación de sesión puede fallar y la reserva se gestiona según el resultado. Webhook y reconciliación evitan depender del retorno del navegador. La integración actual acepta modo prueba.

## Estados del pedido

```mermaid
stateDiagram-v2
    [*] --> PENDING_PAYMENT
    PENDING_PAYMENT --> PAID: Pago verificado
    PENDING_PAYMENT --> EXPIRED: Caducidad confirmada
    PAID --> PREPARING: ADMIN prepara
    PREPARING --> SHIPPED: ADMIN envía
    SHIPPED --> DELIVERED: ADMIN entrega
    EXPIRED --> [*]
    DELIVERED --> [*]
```

No se permite avanzar directamente de pagado a entregado ni retroceder estados. Las repeticiones del mismo estado admitidas por el modelo son idempotentes.

## Estados de devolución

```mermaid
stateDiagram-v2
    [*] --> REQUESTED
    REQUESTED --> IN_REVIEW
    REQUESTED --> AWAITING_ITEMS
    REQUESTED --> REJECTED
    IN_REVIEW --> AWAITING_ITEMS
    IN_REVIEW --> REJECTED
    AWAITING_ITEMS --> RECEIVED
    RECEIVED --> CLOSED
    CLOSED --> [*]
    REJECTED --> [*]
```

CLOSED se muestra como Finalizada y REJECTED como No aceptada; ambos requieren respuesta explicativa. El flujo gestiona la solicitud y no llama a un reembolso automático. Una prenda defectuosa se atiende por correo según la política.

## Recuperación de contraseña

```mermaid
sequenceDiagram
    actor Persona
    participant Web
    participant API
    participant DB
    participant Mail as Correo
    Persona->>Web: Solicita recuperar contraseña
    Web->>API: Email con CSRF
    opt Cuenta apta y solicitud permitida
        API->>DB: Guarda hash del token con caducidad
        API->>Mail: Envía enlace temporal
    end
    API-->>Web: Respuesta genérica
    Persona->>Web: Abre enlace y escribe nueva contraseña
    Web->>API: Token y contraseña con CSRF
    API->>DB: Verifica token vigente y bloquea registro
    API->>DB: Cambia contraseña, consume token y versión de credenciales
    API-->>Web: Cambio completado
```

