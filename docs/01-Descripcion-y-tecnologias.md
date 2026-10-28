# Descripción y tecnologías

Revisión del **28 de octubre de 2026** sobre el código local de las ramas de desarrollo. No se afirma que los cambios estén integrados en main o publicados en GitHub.

El backend es la API de Equisibé: centraliza acceso, catálogo, cuenta, compras, pedidos, devoluciones y administración. Aplica los permisos y calcula los importes que utiliza el frontend.

| Tecnología | Versión/configuración | Función |
|---|---|---|
| Java | 21 | Lenguaje y plataforma |
| Spring Boot | 4.1.1 | Aplicación y configuración |
| Spring Web MVC | Gestionado por Boot | Controladores REST |
| Spring Security / OAuth2 Client | Gestionado por Boot | Sesión, CSRF, roles y Google/OIDC |
| Spring Data JPA / Hibernate | Gestionado por Boot | Persistencia y transacciones |
| MySQL | 8.4 en Compose | Base de desarrollo |
| H2 | Dependencia del proyecto | Base en memoria para pruebas |
| Spring Mail | Gestionado por Boot | SMTP de contacto y recuperación |
| Stripe Java | 34.0.0 | Checkout y avisos en modo prueba |
| JUnit / Mockito / MockMvc | Gestionados por Boot | Pruebas unitarias e integración |
| JaCoCo | 0.8.16 | Cobertura |
| Maven / Surefire | Surefire 3.5.6 | Construcción y ejecución de pruebas |

## Funciones entregadas

Registro/sesión, Google, recuperación de contraseña, catálogo y tallas, favoritos, direcciones, validación de cesta/checkout, pedidos y Stripe de prueba. Administración de catálogo, fotos, stock, rebajas, preparación/envío, devoluciones, promociones y cierre de tienda. Contacto entrega correo al taller.

Las devoluciones permiten solicitar y gestionar unidades de un pedido; no ejecutan reembolso automático ni cambio de talla. La cesta persiste en el navegador por cuenta, no en una tabla del backend. El despliegue público es una fase separada del cierre funcional.
