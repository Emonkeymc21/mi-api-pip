package com.example.posapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Map;

@SpringBootApplication
public class PosApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PosApiApplication.class, args);
    }
}

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final String EXTERNAL_API_URL = "https://servicios.dev.itcsoluciones.ar/pstool/prestadores?telefono=";
    private final String TOKEN = "Bearer token-api-seguro-987654321"; // Usa la variable de entorno en Railway

    @PostMapping
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> request) {
        try {
            String telefono = (String) request.get("telefono");
            String mensaje = (String) request.get("mensaje");

            if ("hola".equalsIgnoreCase(mensaje)) {
                Map<String, Object> responseData = obtenerDatosPrestador(telefono);

                if (responseData != null) {
                    String razonSocial = (String) responseData.get("razon_social");

                    // Extraer el primer contacto disponible
                    var contactos = (Object[]) responseData.get("contactos");
                    String contacto = "Usuario";
                    if (contactos != null && contactos.length > 0) {
                        Map<String, Object> contactoInfo = (Map<String, Object>) contactos[0];
                        contacto = (String) contactoInfo.getOrDefault("observaciones", "Usuario");
                    }

                    String respuesta = "BIENVENIDO " + contacto + " DE " + razonSocial + ", ¿EN QUÉ LO PODEMOS AYUDAR?";
                    return ResponseEntity.ok(Map.of("respuesta", respuesta));
                }
            }
            return ResponseEntity.ok(Map.of("respuesta", "Mensaje no reconocido"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error procesando el mensaje", "detalle", e.getMessage()));
        }
    }

    private Map<String, Object> obtenerDatosPrestador(String telefono) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", TOKEN);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(EXTERNAL_API_URL + telefono, HttpMethod.GET, entity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var dataArray = (Object[]) response.getBody().get("response");
                if (dataArray != null && dataArray.length > 0) {
                    return (Map<String, Object>) dataArray[0];
                }
            }
        } catch (Exception e) {
            System.err.println("Error al obtener datos del prestador: " + e.getMessage());
        }
        return null;
    }
}
