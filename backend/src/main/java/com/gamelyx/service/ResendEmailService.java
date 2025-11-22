package com.gamelyx.service;

import com.gamelyx.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio de email usando Resend API (funciona sobre HTTPS, no requiere puertos SMTP)
 * Resend: 3,000 emails gratis al mes
 * Se usa SOLO en producción (cuando está activo el profile "production")
 */
@Service
@Profile("production")
public class ResendEmailService implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final WebClient webClient;
    private final TemplateEngine templateEngine;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public ResendEmailService(WebClient.Builder webClientBuilder, TemplateEngine templateEngine) {
        this.webClient = webClientBuilder
                .baseUrl(RESEND_API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.templateEngine = templateEngine;
        log.info("ResendEmailService initialized - TemplateEngine: {}",
                templateEngine != null ? "OK" : "NULL");
    }

    /**
     * Envía email de verificación a un usuario recién registrado
     */
    @Override
    public void sendVerificationEmail(User user, String token) {
        log.info("=== INICIO: Enviando email de verificación via Resend ===");
        log.info("Usuario: {} (ID: {}), Email: {}", user.getUsername(), user.getId(), user.getEmail());
        log.info("Token recibido: {} (length: {})", token != null ? "OK" : "NULL", token != null ? token.length() : 0);

        try {
            // Validar configuración
            log.debug("Configuración - fromEmail: {}, fromName: {}, frontendUrl: {}",
                    fromEmail, fromName, frontendUrl);

            if (resendApiKey == null || resendApiKey.isEmpty()) {
                log.error("ERROR CRÍTICO: resendApiKey está vacío o null");
                throw new IllegalStateException("resendApiKey no configurado");
            }

            if (fromEmail == null || fromEmail.isEmpty()) {
                log.error("ERROR CRÍTICO: fromEmail está vacío o null");
                throw new IllegalStateException("fromEmail no configurado");
            }

            if (frontendUrl == null || frontendUrl.isEmpty()) {
                log.error("ERROR CRÍTICO: frontendUrl está vacío o null");
                throw new IllegalStateException("frontendUrl no configurado");
            }

            // Determinar idioma
            Locale locale = Locale.forLanguageTag("es");
            log.debug("Locale configurado: {}", locale);

            // Construir URL de verificación
            String verificationUrl = buildVerificationUrl(token);
            log.info("URL de verificación generada: {}", verificationUrl);

            // Crear contexto para el template
            Context context = new Context(locale);
            context.setVariable("user", user);
            context.setVariable("username", user.getUsername());
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("companyName", "Gamelyx");
            log.debug("Contexto de template creado con {} variables", context.getVariableNames().size());

            // Procesar template HTML
            log.debug("Procesando template: email/verification");
            String htmlContent = templateEngine.process("email/verification", context);
            log.info("Template procesado exitosamente - Longitud HTML: {} caracteres", htmlContent.length());

            // Enviar email via Resend API
            log.info("Intentando enviar email a: {} via Resend API", user.getEmail());
            sendEmail(
                    user.getEmail(),
                    "Verifica tu cuenta en Gamelyx",
                    htmlContent
            );

            log.info("✓ Email de verificación enviado EXITOSAMENTE a: {}", user.getEmail());
            log.info("=== FIN: Email de verificación completado ===");

        } catch (Exception e) {
            log.error("✗ ERROR ENVIANDO EMAIL DE VERIFICACIÓN", e);
            log.error("Detalles del error:");
            log.error("  - Tipo: {}", e.getClass().getName());
            log.error("  - Mensaje: {}", e.getMessage());
            log.error("  - Usuario: {}", user != null ? user.getEmail() : "NULL");
            log.error("  - Token presente: {}", token != null);
            if (e.getCause() != null) {
                log.error("  - Causa raíz: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            throw new RuntimeException("Error al enviar email de verificación a " + user.getEmail(), e);
        }
    }

    /**
     * Envía email de bienvenida tras verificación exitosa
     */
    @Override
    public void sendWelcomeEmail(User user) {
        log.info("=== INICIO: Enviando email de bienvenida via Resend ===");
        log.info("Usuario: {} (ID: {}), Email: {}", user.getUsername(), user.getId(), user.getEmail());

        try {
            Locale locale = Locale.forLanguageTag("es");
            String loginUrl = frontendUrl + "/login";
            log.debug("Login URL generada: {}", loginUrl);

            Context context = new Context(locale);
            context.setVariable("user", user);
            context.setVariable("username", user.getUsername());
            context.setVariable("loginUrl", loginUrl);
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("companyName", "Gamelyx");

            log.debug("Procesando template: email/welcome");
            String htmlContent = templateEngine.process("email/welcome", context);
            log.info("Template procesado - Longitud HTML: {} caracteres", htmlContent.length());

            log.info("Enviando email de bienvenida a: {} via Resend API", user.getEmail());
            sendEmail(
                    user.getEmail(),
                    "¡Bienvenido a Gamelyx!",
                    htmlContent
            );

            log.info("✓ Email de bienvenida enviado EXITOSAMENTE a: {}", user.getEmail());
            log.info("=== FIN: Email de bienvenida completado ===");

        } catch (Exception e) {
            log.error("✗ ERROR ENVIANDO EMAIL DE BIENVENIDA", e);
            log.error("Usuario: {}, Error: {}", user != null ? user.getEmail() : "NULL", e.getMessage());
            if (e.getCause() != null) {
                log.error("Causa raíz: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Error al enviar email de bienvenida a " + user.getEmail(), e);
        }
    }

    /**
     * Método para testing - envía email de prueba
     */
    @Override
    public void sendTestEmail(String to) {
        log.info("=== INICIO: Enviando email de PRUEBA via Resend ===");
        log.info("Destinatario: {}", to);

        try {
            Context context = new Context();
            context.setVariable("message", "Este es un email de prueba desde Gamelyx via Resend");
            context.setVariable("timestamp", java.time.LocalDateTime.now().toString());

            log.debug("Procesando template: email/test");
            String htmlContent = templateEngine.process("email/test", context);
            log.info("Template procesado - Longitud HTML: {} caracteres", htmlContent.length());

            log.info("Enviando email de prueba a: {} via Resend API", to);
            sendEmail(to, "Test Email - Gamelyx (Resend)", htmlContent);

            log.info("✓ Email de PRUEBA enviado EXITOSAMENTE a: {}", to);
            log.info("=== FIN: Email de prueba completado ===");

        } catch (Exception e) {
            log.error("✗ ERROR ENVIANDO EMAIL DE PRUEBA", e);
            log.error("Destinatario: {}, Error: {}", to, e.getMessage());
            if (e.getCause() != null) {
                log.error("Causa raíz: {}", e.getCause().getMessage());
            }
            throw new RuntimeException("Error al enviar email de prueba a " + to, e);
        }
    }

    /**
     * Método genérico para enviar emails via Resend API
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        log.debug("--- Preparando email via Resend API ---");
        log.debug("  Destinatario: {}", to);
        log.debug("  Asunto: {}", subject);
        log.debug("  From: {} <{}>", fromName, fromEmail);
        log.debug("  Contenido HTML: {} caracteres", htmlContent != null ? htmlContent.length() : 0);

        try {
            // Construir el body de la request según la API de Resend
            Map<String, Object> requestBody = Map.of(
                    "from", fromName + " <" + fromEmail + ">",
                    "to", new String[]{to},
                    "subject", subject,
                    "html", htmlContent
            );

            log.debug("Enviando request a Resend API...");
            long startTime = System.currentTimeMillis();

            // Llamar a la API de Resend
            Map<String, Object> response = webClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
                    .body(Mono.just(requestBody), Map.class)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            long duration = System.currentTimeMillis() - startTime;

            log.info("✓ Email enviado EXITOSAMENTE via Resend en {} ms", duration);
            log.debug("Resend response: {}", response);

        } catch (WebClientResponseException e) {
            log.error("✗ Error HTTP de Resend API", e);
            log.error("  Status code: {}", e.getStatusCode());
            log.error("  Response body: {}", e.getResponseBodyAsString());
            log.error("  Destinatario: {}", to);
            log.error("  Asunto: {}", subject);
            throw new RuntimeException("Error al enviar email via Resend API: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("✗ Error inesperado al enviar email via Resend", e);
            log.error("  Destinatario: {}", to);
            log.error("  Tipo de excepción: {}", e.getClass().getName());
            log.error("  Mensaje: {}", e.getMessage());
            throw new RuntimeException("Error inesperado al enviar email via Resend", e);
        }
    }

    /**
     * Construye la URL de verificación de email
     */
    private String buildVerificationUrl(String token) {
        return frontendUrl + "/auth/verify-email?token=" + token;
    }
}
