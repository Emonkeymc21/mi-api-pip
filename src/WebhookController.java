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

import java.util.*;

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
    private final String TOKEN = "Bearer eyJraWQiOiJzLWI3NTUwNjBkLTJlYzUtNGQ5Ni05YWMxLTBhMjE4MjJjMWU5NiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJpYXQiOjE3NDE2OTQyODksImV4cCI6Mzg4OTE3NzkzNiwiaXNzIjoiaHR0cDovL2FwaS92MS9tcG9zYmFja2VuZC9zdXBlcnRva2VucyIsInNvdXJjZSI6InRva2VuUGlwIiwibm9Vc2FyVG9rZW4iOiJub1VzYXJUb2tlbiIsInVzZXIiOiJmYWxzbyJ9.UCLTS9roWvrBzEGKl8wvEmPGEXd00GoosffQiV_EwRMdLuMjOHvh9dzN4a94jXNRsLD7zVXNXz2dWTeR88urWNdOR4cI3_mVTcIm6zwIEHX4U3C3Gw2uj5kKqNv4ltYhZVnpqPClcVr7xh6f8wpnpY6NrBXvXxk6ivEe0ELLqQ5KfwtU3mROxS0hmG1nKpm9yg3G-S1t0XiFskna5DIyNFPYCO_yvGMw-QsAmVexTDCrrqiY4SM_2SG4va8SjXBXONMwCAesp0RVr0sntQc1qIlayPO19SZuHNBfrwJ4GwRbC-UyTeLx9P1d93QT0UnomcMcMit3HfmKey9FjnBDnA";

    @PostMapping
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> request) {
        try {
            String telefono = (String) request.get("telefono");
            String mensaje = (String) request.get("mensaje");

            if ("hola".equalsIgnoreCase(mensaje)) {
                List<Map<String, Object>> responseData = obtenerDatosPrestador(telefono);

                if (responseData != null && !responseData.isEmpty()) {
                    List<String> opciones = new ArrayList<>();
                    for (Map<String, Object> prestador : responseData) {
                        String razonSocial = (String) prestador.get("razon_social");
                        opciones.add(razonSocial);
                    }

                    if (opciones.size() > 1) {
                        return ResponseEntity.ok(Map.of("respuesta", "BIENVENIDO, ¿CUAL ES EL PRESTADOR POR EL QUE SE COMUNICA? " + String.join(", ", opciones)));
                    }
                    
                    Map<String, Object> prestadorSeleccionado = responseData.get(0);
                    String razonSocial = (String) prestadorSeleccionado.get("razon_social");
                    List<Map<String, Object>> contactosList = (List<Map<String, Object>>) prestadorSeleccionado.get("contactos");
                    String contacto = contactosList != null && !contactosList.isEmpty() ? (String) contactosList.get(0).get("observaciones") : "Usuario";

                    String respuesta = "BIENVENIDO " + contacto + " DE " + razonSocial + ", ¿EN QUÉ LO PODEMOS AYUDAR?";
                    return ResponseEntity.ok(Map.of("respuesta", respuesta));
                }
            }
            return ResponseEntity.ok(Map.of("respuesta", "Mensaje no reconocido"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error procesando el mensaje", "detalle", e.getMessage()));
        }
    }

    private List<Map<String, Object>> obtenerDatosPrestador(String telefono) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", TOKEN);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(EXTERNAL_API_URL + telefono, HttpMethod.GET, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getBody() != null && response.getBody().containsKey("response")) {
                Object rawResponse = response.getBody().get("response");
                List<Map<String, Object>> responseData = new ArrayList<>();

                if (rawResponse instanceof List<?>) {
                    for (Object obj : (List<?>) rawResponse) {
                        if (obj instanceof Map<?, ?>) {
                            responseData.add((Map<String, Object>) obj);
                        }
                    }
                }
                return responseData;
            }
        } catch (Exception e) {
            System.err.println("Error al obtener datos del prestador: " + e.getMessage());
        }
        return Collections.emptyList();
    }
}
