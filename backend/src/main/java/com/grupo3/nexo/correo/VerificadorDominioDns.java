package com.grupo3.nexo.correo;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consulta DNS: el dominio debe tener registro MX (o, como indica el estándar, al menos A/AAAA).
 * Si el DNS no responde (red caída, tiempo agotado) NO se rechaza al usuario: se deja pasar y se avisa en el log,
 * porque la confirmación por código sigue probando que el correo existe.
 */
@Component
@ConditionalOnProperty(name = "nexo.correo.verificar-mx", havingValue = "true", matchIfMissing = true)
public class VerificadorDominioDns implements VerificadorDominioCorreo {

    private static final Logger log = LoggerFactory.getLogger(VerificadorDominioDns.class);
    private static final String[] TIPOS = {"MX", "A", "AAAA"};

    @Override
    public boolean dominioRecibeCorreo(String dominio) {
        Hashtable<String, String> entorno = new Hashtable<>();
        entorno.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        entorno.put("com.sun.jndi.dns.timeout.initial", "2000");
        entorno.put("com.sun.jndi.dns.timeout.retries", "2");
        try {
            DirContext contexto = new InitialDirContext(entorno);
            try {
                for (String tipo : TIPOS) {
                    Attributes atributos = contexto.getAttributes(dominio, new String[] {tipo});
                    if (atributos.get(tipo) != null && atributos.get(tipo).size() > 0) {
                        return true;
                    }
                }
                return false;
            } finally {
                contexto.close();
            }
        } catch (NameNotFoundException e) {
            return false;
        } catch (NamingException e) {
            log.warn("No se pudo consultar el DNS de {}: {}. Se deja pasar.", dominio, e.getMessage());
            return true;
        }
    }
}
