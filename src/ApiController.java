package com.example.api;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> userCache = new HashMap<>();
    
    // ?? Token seguro de la API (para validar que la solicitud venga del CRM de WhatsApp)
    private final String API_TOKEN = "token-api-seguro-987654321";
    
    // ?? URL de la API externa que consulta la razón social
    private final String apiUrl = "https://servicios.dev.itcsoluciones.ar/pstool/prestadores?telefono=";
    
    // ?? Token para acceder a la API externa
    private final String externalToken = "eyJraWQiOiJzLWI3NTUwNjBkLTJlY..."; 

    @PostMapping("/whatsapp-webhook")
    public String recibirMensajeWhatsApp(@RequestHeader(value = "Authorization", required = false) String token,
                                         @RequestBody Map<String, Object> request) {

        // ?? Verificar el token de autenticación
        if (token == null || !token.equals("Bearer " + API_TOKEN)) {
            return "Acceso no autorizado. Token inválido.";
        }

        // ?? Extraer el número de WhatsApp desde el webhook
        if (!request.containsKey("telefono")) {
            return "Error: No se recibió un número de WhatsApp.";
        }

        String telefono = request.get("telefono").toString();

        // ?? Verificar si ya se consultó antes
        if (userCache.containsKey(telefono)) {
            return "Bienvenido nuevamente, " + userCache.get(telefono) + ". ¿Cuál es su inconveniente de hoy?";
        }

        // ?? Consultar la API externa para obtener la razón social
        String razonSocial = obtenerRazonSocial(telefono);
        
        if (razonSocial == null) {
            return "No se encontró información para el número proporcionado.";
        }

        // ?? Guardar en caché la razón social para futuras consultas
        userCache.put(telefono, razonSocial);
        
        return "Bienvenido " + razonSocial + ", ¿cuál es su inconveniente de hoy?";
    }

    private String obtenerRazonSocial(String telefono) {
        try {
            String url = apiUrl + telefono;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + externalToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("razon_social")) {
                return response.getBody().get("razon_social").toString();
            }
        } catch (Exception e) {
            System.err.println("Error al consultar la API externa: " + e.getMessage());
        }
        return null;
    }
}