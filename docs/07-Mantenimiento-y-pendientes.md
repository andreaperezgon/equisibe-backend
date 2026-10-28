# Mantenimiento y cierre

## Datos y archivos

El volumen equisibe_mysql_data conserva MySQL. uploads/products conserva las imágenes subidas, fuera de Git; respaldar ambas cosas. Guardar solo la base no conserva las fotos. No borrar el volumen con down -v para reiniciar un servicio normal.

Las imágenes estáticas de la web pertenecen al build del frontend. Las URL guardadas en productos no contienen los bytes de las imágenes.

## Operación

| Síntoma | Comprobación |
|---|---|
| Puerto 8080 ocupado | Detener el backend anterior; no superponer normal y test,e2e |
| MySQL no conecta | Docker, puerto 3306 y volumen/configuración |
| Error al iniciar Stripe/reconciliación | Clave de pruebas, conectividad y listener si se validan pagos |
| Contacto/recuperación no envían | APP_MAIL_ENABLED, SMTP y variables recibidas por el proceso |
| Google no inicia | Perfil google, cliente, secreto y URI autorizada exacta |
| Código bloqueado | Habilitación y popup activo con el mismo código |
| Cantidad/estado rechazado | Revisar respuesta 400/409 y operación permitida |
| Cobertura vacía o clases diferentes | Ejecutar clean verify al terminar procesos anteriores |

## Alcance cerrado

La entrega incluye devoluciones y seguimiento; no reembolso automático. No hay cambio de talla ni vinculación manual de Google implementados como funciones adicionales. La tienda reabre manualmente tras la cuenta atrás.

## Antes de desplegar

La entrega local no equivale a publicación. Configurar HTTPS, dominios, URL y callbacks (incluida la redirección fija de GoogleLoginHandler), cookies/CORS, secretos, almacenamiento persistente y copias verificadas. Sustituir credenciales locales, separar entornos y planificar migraciones de esquema en lugar de confiar en ddl-auto=update.

Stripe actual está limitado a pruebas. El correo de dominio propio y el proveedor de alojamiento no están contratados como parte de esta entrega. Completar la información de privacidad según los servicios elegidos. No se afirma cumplimiento legal completo por tener una página de política.

Documentación, fuentes e informes tienen finalidades distintas: conservar los fuentes de pruebas y estas guías; regenerar target/coverage cuando sea necesario. Los commits, push y merge los realiza la persona responsable.
