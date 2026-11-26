# CardioCode
Telemedicine system for remote monitoring of atrial fibrillation patients. Java client-server app with BITalino ECG acquisition, symptom reporting, SQLite storage and patient/doctor JavaFX clients.

## Requisitos
| | |
|---|---|
| Java | JDK 21 o superior (probado con 21 y 25) |
| Maven | 3.9+ (o el Maven integrado de IntelliJ) |
| Sistemas operativos | macOS (Intel y Apple Silicon), Windows 10/11, Linux x64 |
| Librerías (las descarga Maven) | JavaFX 21.0.2, sqlite-jdbc 3.46.1.3, BlueCove 2.1.1, JUnit 5 |
| Hardware | BITalino con Bluetooth emparejado con el ordenador del paciente |

## Compilar
```bash
mvn clean package
```
Genera en `target/` dos JAR ejecutables que ya incluyen sus dependencias:

| JAR | Aplicación | Contiene |
|---|---|---|
| `CardioCode-server.jar` | Servidor del hospital (`server.HospitalServer`) | SQLite |
| `CardioCode-client.jar` | Cliente para pacientes y médicos (`gui.GUI`) | JavaFX + BlueCove |

El JAR del cliente lleva las librerías nativas de JavaFX **del sistema donde se compila**.
Para generarlo para otro sistema:
```bash
mvn clean package -Djavafx.platform=win      # Windows
mvn clean package -Djavafx.platform=mac      # macOS Intel
mvn clean package -Djavafx.platform=mac-aarch64   # macOS Apple Silicon
mvn clean package -Djavafx.platform=linux    # Linux
```

## Ejecutar
### Servidor
```bash
java -jar target/CardioCode-server.jar
```
- Crea `db/cardiocode.db`, `db/admin.key` y `recordings/` en la carpeta desde la que se lanza.
- La primera vez pide crear la contraseña de administrador (se guarda con hash PBKDF2 en `db/admin.key`).
- Escucha en el puerto TCP **6767**.

### Cliente (paciente / médico)
```bash
java -jar target/CardioCode-client.jar
```

### Desde Maven, sin generar JAR
```bash
mvn compile exec:java     # servidor
mvn javafx:run            # cliente
```

Con Java 24 o superior aparecen avisos `WARNING: A restricted method ... has been called`. Son inofensivos; para ocultarlos se puede añadir `--enable-native-access=ALL-UNNAMED` antes de `-jar`.
