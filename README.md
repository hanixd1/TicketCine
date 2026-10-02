# Cine Amazonas

Aplicación Java 17 con Spring Boot, JSP y H2. No utiliza JavaScript, Thymeleaf, JPA ni Hibernate. Se conserva la paleta original; las pantallas nuevas son la sala de asientos y el acceso administrativo independiente.

## Ejecutar desde el código

Instala un JDK 17 o superior. No necesitas instalar Maven, Tomcat ni un servidor de base de datos.

En macOS o Linux, desde la carpeta del proyecto:

```sh
./mvnw clean package
java -Xms64m -Xmx256m -jar target/cine-amazonas.war
```

En Windows:

```bat
mvnw.cmd clean package
java -Xms64m -Xmx256m -jar target\cine-amazonas.war
```

La primera compilación necesita internet para descargar las dependencias. Abre http://localhost:8080. Para usar otro puerto añade `--server.port=8081` al comando de Java. Detén la aplicación antes de volver a compilar el WAR que esté usando.

## Llevarlo a la máquina del docente

Compila primero y copia únicamente `target/cine-amazonas.war` a una carpeta del docente. Allí, con Java 17 o superior instalado, ejecuta:

```sh
java -Xms64m -Xmx256m -jar cine-amazonas.war
```

El WAR incluye Spring, Tomcat, JSP y H2; no requiere Maven ni descargar dependencias en esa máquina. Las fuentes de Google necesitan internet para verse exactamente igual; sin conexión se usan las fuentes alternativas del CSS original.

Si debes entregar el código, incluye `src`, `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn` y este README. No incluyas el resto de `target`, `.idea` ni los datos de tus pruebas. El WAR es ejecutable, no un JAR, conforme al soporte de JSP documentado por [Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/web/servlet.html#web.servlet.embedded-container.jsp-limitations).

## Cuentas de demostración

| Rol | Correo | Contraseña |
| --- | --- | --- |
| ADMIN | admin@cineamazonas.pe | Admin123! |
| USER | cliente@cineamazonas.pe | Cliente123! |

Estas cuentas son solo para la demostración local. Los clientes ingresan en `/login`; los administradores escriben `/admin`, que los lleva al acceso administrativo independiente si falta su sesión. No hay enlaces administrativos en las pantallas públicas. Cada acceso rechaza las cuentas del otro rol. El registro público siempre crea un USER. Las contraseñas se guardan como resúmenes PBKDF2 con sal, no como texto legible. El panel exige una sesión ADMIN; “Mis tickets” solo aparece para clientes con sesión USER y consulta únicamente sus entradas. Escribir su URL sin una sesión de cliente también requiere login.

## Comprar asientos

1. En la cartelera pulsa **Comprar entrada** en el horario elegido. Se abre la sala, sin realizar una compra.
2. Marca uno o más asientos libres y pulsa **Continuar**. Java valida la selección y la conserva temporalmente en la sesión; todavía no guarda tickets en H2. Hay diez posiciones por fila, un pasillo central y tantos asientos como el aforo de la sala. No hay asientos especiales.
3. Si no has iniciado sesión como cliente, elige **Ingresar** o **Seguir como invitado**. Ambas opciones llevan al resumen con los mismos asientos. Un cliente que ya inició sesión pasa directamente al resumen.
4. Revisa la función, los asientos y el total y pulsa **Confirmar compra**. Solo entonces Java vuelve a comprobar disponibilidad y guarda una entrada por asiento. Con una cuenta, las entradas aparecen en **Mis tickets**. Como invitado, se muestra el comprobante de esa compra, sin dar acceso al historial.

Los pasos se resuelven con formularios y redirecciones: `GET /sala` muestra el mapa, `POST /seleccionar` prepara la selección, el acceso elegido devuelve a `GET /confirmar-compra` y `POST /comprar` guarda la compra. Abrir otra sala reemplaza la selección pendiente. Los asientos no se bloquean durante el login: si alguien los compra antes de confirmar, se informa el conflicto y se pueden elegir otros.

La compra de invitado no crea usuarios: sus entradas tienen `usuario_id` vacío y la compra se identifica con un código aleatorio de su sesión. Solo ese navegador y sesión pueden consultar el comprobante. Guarda una captura o imprímelo con el navegador antes de cerrar la sesión. Aunque el comprobante deje de estar accesible, los asientos comprados permanecen guardados en H2. Ingresar después no agrega automáticamente esas compras al historial de una cuenta.

La selección se marca con CSS, no con JavaScript; Java calcula el total para el resumen. Los ocupados no se pueden marcar. La ocupación es por función: A1 puede estar ocupado en un horario y libre en otro. Los asientos se generan con bucles a partir del aforo, sin una clase de asiento.

La compra completa se guarda en una transacción JDBC: o se guardan todos los asientos, o ninguno. Se bloquea la función mientras se comprueba disponibilidad; un índice único en H2 también impide duplicar un asiento activo. Reenviar el mismo formulario no duplica entradas. El comportamiento del bloqueo y del índice corresponde a la [documentación de H2](https://h2database.com/html/commands.html#select).

Es una compra de demostración registrada localmente: no procesa tarjetas ni pagos bancarios.

## Base de datos

H2 crea `data/cine-amazonas.mv.db` dentro de la carpeta desde la que ejecutas el programa. Las cuentas nuevas y los cambios SQL persisten al cerrar y volver a abrir. No hay que instalar MySQL ni PostgreSQL.

`src/main/resources/schema.sql` define las tablas y añade las columnas nuevas a las bases existentes; `data.sql` carga los datos de ejemplo sin borrar los registros. Las funciones iniciales se programan para el día del primer arranque; si reutilizas la base otro día debes ajustar sus fechas para que aparezcan como funciones de hoy. La migración conserva los tickets anteriores; a los tres tickets de demostración se les asignan los asientos B5, A8 y C1 en sus respectivas funciones.

Se conservó una copia previa a los cambios en `data/respaldo-anterior.mv.db`. No es necesaria para ejecutar ni entregar la aplicación; mantenla como respaldo local. La base en uso es `data/cine-amazonas.mv.db`.

Para llevar también tus registros al docente, detén la aplicación y copia la carpeta `data` junto al WAR. No compartas datos personales. Para consultar el archivo con una herramienta JDBC, cierra primero la aplicación y usa la URL `jdbc:h2:file:/ruta/absoluta/data/cine-amazonas`, usuario `sa` y contraseña vacía. Utiliza la misma versión de H2 incluida en el WAR. No se habilita una consola web adicional.

## Organización para explicarlo

- `CineAmazonasApplication.java`: arranque de Spring.
- `AuthController.java`: accesos separados por rol, continuación como invitado, registro, validaciones y cierre de sesión.
- `CarteleraController.java`: cartelera, mapa por bucles, compra transaccional y comprobante privado del invitado.
- `NavegacionController.java`: rutas públicas y consulta de tickets; un bucle `for` prepara los números y estados.
- `AdminController.java`: comprueba el rol y consulta los catálogos.
- `src/main/webapp/WEB-INF/jsp`: pantallas renderizadas en el servidor. JSTL repite elementos y evalúa condiciones; no es JavaScript.
- `cabecera.jspf`: encabezado compartido por las pantallas públicas; una condición muestra “Mis tickets” únicamente al cliente autenticado. Evita repetir la navegación en cada pantalla.
- `src/main/resources/static/css`: estilos con la misma paleta del login.

La lógica utiliza tipos primitivos (`int`, `long`, `boolean`, `byte`), variables, arreglos, las interfaces de colecciones `List` y `Map`, bucles y condicionales. No hay lambdas, streams, clases de entidades, repositorios propios, herencia de dominio ni patrones POO añadidos. Java y Spring sí requieren las clases de arranque/controladores y objetos de biblioteca: un proyecto Spring completamente sin clases y objetos no es posible. No se añaden interfaces Java propias que no sean necesarias.

## Alcance conservado

El login, el registro, los catálogos, el mapa de asientos y las compras trabajan desde Spring y H2. La marca, el logo con iniciales CA, el nombre de la aplicación, los paquetes Java y el WAR se corresponden con Cine Amazonas.

Los botones de crear/editar/desactivar catálogos, recuperar contraseña y enviar contacto siguen sin implementación. Los indicadores y gráficos del dashboard continúan siendo ejemplos visuales, no estadísticas reales; la tabla de funciones sí muestra la ocupación leída desde H2. No hay una pasarela de pago ni un flujo de cancelación de compras.

## Verificar

```sh
./mvnw test
```

Las 32 pruebas usan bases H2 en memoria independientes; no alteran la base de la aplicación. Comprueban registro, validaciones, credenciales, separación de roles, sesiones, formularios, selección sin compra automática, elección de acceso, conservación de asientos tras el login, compras con cuenta y como invitado, aforo, ocupación al confirmar, reenvíos y privacidad de tickets y comprobantes.
