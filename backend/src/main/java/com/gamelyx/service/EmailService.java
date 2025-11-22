package com.gamelyx.service;

import com.gamelyx.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

/**
 * Servicio de email usando Gmail SMTP
 * Se usa SOLO en desarrollo local (cuando NO está el profile "production")
 */
@Service
@Profile("!production")
public class EmailService implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        log.info("EmailService initialized - MailSender: {}, TemplateEngine: {}",
            mailSender != null ? "OK" : "NULL",
            templateEngine != null ? "OK" : "NULL");
    }

    /**
     * Envía email de verificación a un usuario recién registrado
     */
    @Override
    public void sendVerificationEmail(User user, String token) {
        log.info("=== INICIO: Enviando email de verificación ===");
        log.info("Usuario: {} (ID: {}), Email: {}", user.getUsername(), user.getId(), user.getEmail());
        log.info("Token recibido: {} (length: {})", token != null ? "OK" : "NULL", token != null ? token.length() : 0);

        try {
            // Validar configuración
            log.debug("Configuración - fromEmail: {}, fromName: {}, frontendUrl: {}",
                fromEmail, fromName, frontendUrl);

            if (fromEmail == null || fromEmail.isEmpty()) {
                log.error("ERROR CRÍTICO: fromEmail está vacío o null");
                throw new IllegalStateException("fromEmail no configurado");
            }

            if (frontendUrl == null || frontendUrl.isEmpty()) {
                log.error("ERROR CRÍTICO: frontendUrl está vacío o null");
                throw new IllegalStateException("frontendUrl no configurado");
            }

            // Determinar idioma (por defecto español, pero preparado para multiidioma)
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

            // Enviar email
            log.info("Intentando enviar email a: {}", user.getEmail());
            sendHtmlEmail(
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
            // Re-lanzar la excepción para que el llamador pueda manejarla
            throw new RuntimeException("Error al enviar email de verificación a " + user.getEmail(), e);
        }
    }

    /**
     * Envía email de bienvenida tras verificación exitosa
     */
    @Override
    public void sendWelcomeEmail(User user) {
        log.info("=== INICIO: Enviando email de bienvenida ===");
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

            log.info("Enviando email de bienvenida a: {}", user.getEmail());
            sendHtmlEmail(
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
     * Envía email de restablecimiento de contraseña
     * NOTA: Esta funcionalidad NO está actualmente implementada en el sistema
     */
    public void sendPasswordResetEmail(User user, String resetToken) {
        try {
            Locale locale = Locale.forLanguageTag("es");

            Context context = new Context(locale);
            context.setVariable("user", user);
            context.setVariable("username", user.getUsername());
            context.setVariable("resetUrl", buildPasswordResetUrl(resetToken));
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("companyName", "Gamelyx");

            String htmlContent = templateEngine.process("email/password-reset", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "Restablecer contraseña - Gamelyx",
                    htmlContent
            );

            System.out.println("Email de restablecimiento enviado a: " + user.getEmail());

        } catch (Exception e) {
            System.err.println("Error enviando email de restablecimiento: " + e.getMessage());
        }
    }

    /**
     * Método genérico para enviar emails HTML
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        log.debug("--- Preparando email ---");
        log.debug("  Destinatario: {}", to);
        log.debug("  Asunto: {}", subject);
        log.debug("  From Email: {}", fromEmail);
        log.debug("  From Name: {}", fromName);
        log.debug("  Contenido HTML: {} caracteres", htmlContent != null ? htmlContent.length() : 0);

        try {
            log.debug("Creando MimeMessage...");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            log.debug("Configurando remitente: {} <{}>", fromName, fromEmail);
            helper.setFrom(fromEmail, fromName);

            log.debug("Configurando destinatario: {}", to);
            helper.setTo(to);

            log.debug("Configurando asunto: {}", subject);
            helper.setSubject(subject);

            log.debug("Configurando contenido HTML");
            helper.setText(htmlContent, true); // true = es HTML

            log.debug("Enviando mensaje a través de JavaMailSender...");
            long startTime = System.currentTimeMillis();
            mailSender.send(message);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Email enviado EXITOSAMENTE en {} ms", duration);

        } catch (java.io.UnsupportedEncodingException e) {
            log.warn("UnsupportedEncodingException al configurar nombre del remitente, usando fallback sin nombre", e);
            log.warn("fromName problemático: {}", fromName);

            // Fallback: usar solo el email sin nombre personalizado
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            log.debug("Configurando remitente (solo email): {}", fromEmail);
            helper.setFrom(fromEmail); // Solo email, sin nombre
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            log.debug("Enviando mensaje (fallback) a través de JavaMailSender...");
            long startTime = System.currentTimeMillis();
            mailSender.send(message);
            long duration = System.currentTimeMillis() - startTime;

            log.info("Email enviado (fallback) en {} ms", duration);

        } catch (MessagingException e) {
            log.error("MessagingException al enviar email", e);
            log.error("  Destinatario: {}", to);
            log.error("  Asunto: {}", subject);
            log.error("  Tipo de excepción: {}", e.getClass().getName());
            log.error("  Mensaje de error: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("  Causa: {} - {}", e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            throw e;

        } catch (Exception e) {
            log.error("Error inesperado al enviar email", e);
            log.error("  Destinatario: {}", to);
            log.error("  Tipo de excepción: {}", e.getClass().getName());
            log.error("  Mensaje: {}", e.getMessage());
            throw new MessagingException("Error inesperado al enviar email", e);
        }
    }


    /**
     * Construye la URL de verificación de email
     */
    private String buildVerificationUrl(String token) {
        return frontendUrl + "/auth/verify-email?token=" + token;
    }

    /**
     * Construye la URL de restablecimiento de contraseña
     */
    private String buildPasswordResetUrl(String token) {
        return frontendUrl + "/auth/reset-password?token=" + token;
    }

    /**
     * Método para testing - envía email de prueba
     */
    @Override
    public void sendTestEmail(String to) {
        log.info("=== INICIO: Enviando email de PRUEBA ===");
        log.info("Destinatario: {}", to);

        try {
            Context context = new Context();
            context.setVariable("message", "Este es un email de prueba desde Gamelyx");
            context.setVariable("timestamp", java.time.LocalDateTime.now().toString());

            log.debug("Procesando template: email/test");
            String htmlContent = templateEngine.process("email/test", context);
            log.info("Template procesado - Longitud HTML: {} caracteres", htmlContent.length());

            log.info("Enviando email de prueba a: {}", to);
            sendHtmlEmail(to, "Test Email - Gamelyx", htmlContent);

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
}