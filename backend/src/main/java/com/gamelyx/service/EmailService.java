package com.gamelyx.service;

import com.gamelyx.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.name}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    /**
     * Envía email de verificación a un usuario recién registrado
     */
    public void sendVerificationEmail(User user, String token) {
        try {
            // Determinar idioma (por defecto español, pero preparado para multiidioma)
            Locale locale = Locale.forLanguageTag("es"); // Por ahora español por defecto

            // Crear contexto para el template
            Context context = new Context(locale);
            context.setVariable("user", user);
            context.setVariable("username", user.getUsername());
            context.setVariable("verificationUrl", buildVerificationUrl(token));
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("companyName", "Gamelyx");

            // Procesar template HTML
            String htmlContent = templateEngine.process("email/verification", context);

            // Enviar email
            sendHtmlEmail(
                    user.getEmail(),
                    "Verifica tu cuenta en Gamelyx",
                    htmlContent
            );

            System.out.println("Email de verificación enviado a: " + user.getEmail());

        } catch (Exception e) {
            System.err.println("Error enviando email de verificación: " + e.getMessage());
            // En producción, podrías loggear esto en un sistema de monitoring
            // o reencolar el email para reintentar más tarde
        }
    }

    /**
     * Envía email de bienvenida tras verificación exitosa
     */
    public void sendWelcomeEmail(User user) {
        try {
            Locale locale = Locale.forLanguageTag("es");

            Context context = new Context(locale);
            context.setVariable("user", user);
            context.setVariable("username", user.getUsername());
            context.setVariable("loginUrl", frontendUrl + "/login");
            context.setVariable("frontendUrl", frontendUrl);
            context.setVariable("companyName", "Gamelyx");

            String htmlContent = templateEngine.process("email/welcome", context);

            sendHtmlEmail(
                    user.getEmail(),
                    "¡Bienvenido a Gamelyx!",
                    htmlContent
            );

            System.out.println("Email de bienvenida enviado a: " + user.getEmail());

        } catch (Exception e) {
            System.err.println("Error enviando email de bienvenida: " + e.getMessage());
        }
    }

    /**
     * Envía email de restablecimiento de contraseña
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = es HTML

            mailSender.send(message);

        } catch (java.io.UnsupportedEncodingException e) {
            // Fallback: usar solo el email sin nombre personalizado
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail); // Solo email, sin nombre
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.err.println("Warning: Usando email sin nombre personalizado debido a encoding");
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
    public void sendTestEmail(String to) {
        try {
            Context context = new Context();
            context.setVariable("message", "Este es un email de prueba desde Gamelyx");
            context.setVariable("timestamp", java.time.LocalDateTime.now().toString());

            String htmlContent = templateEngine.process("email/test", context);

            sendHtmlEmail(to, "Test Email - Gamelyx", htmlContent);

            System.out.println("Email de prueba enviado a: " + to);

        } catch (Exception e) {
            System.err.println("Error enviando email de prueba: " + e.getMessage());
        }
    }
}