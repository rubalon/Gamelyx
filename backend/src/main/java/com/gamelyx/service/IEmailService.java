package com.gamelyx.service;

import com.gamelyx.entity.User;

/**
 * Interfaz común para servicios de email
 * Permite intercambiar entre EmailService (SMTP) y ResendEmailService (API) según el perfil activo
 */
public interface IEmailService {

    /**
     * Envía email de verificación a un usuario recién registrado
     */
    void sendVerificationEmail(User user, String token);

    /**
     * Envía email de bienvenida tras verificación exitosa
     */
    void sendWelcomeEmail(User user);

    /**
     * Método para testing - envía email de prueba
     */
    void sendTestEmail(String to);
}
