package com.benz.gymdesk360.security;

import com.benz.gymdesk360.model.User;
import com.benz.gymdesk360.model.enums.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_GYM = "gymId";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        // Lanza WeakKeyException al arrancar si la clave tiene menos de 256 bits
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .issuer(props.issuer())
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTtl())));
        if (user.getGymId() != null) {
            builder.claim(CLAIM_GYM, user.getGymId());
        }
        return builder.signWith(key).compact();
    }

    /**
     * Valida firma, emisor y expiración.
     *
     * @throws JwtException si el token no es válido
     */
    public AuthUser parseAccessToken(String token) {
        Claims c = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Number gym = c.get(CLAIM_GYM, Number.class);
        return new AuthUser(
                Long.valueOf(c.getSubject()),
                gym == null ? null : gym.longValue(),
                c.get(CLAIM_EMAIL, String.class),
                Role.valueOf(c.get(CLAIM_ROLE, String.class)));
    }

    public long accessTtlSeconds() {
        return props.accessTtl().toSeconds();
    }
}
