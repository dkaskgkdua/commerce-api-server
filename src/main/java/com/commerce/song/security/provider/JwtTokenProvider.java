package com.commerce.song.security.provider;

import com.commerce.song.domain.dto.TokenDto;
import com.commerce.song.domain.entity.Account;
import com.commerce.song.security.common.AccountContext;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String AUTHORITIES_KEY = "auth";
    private static final String BEARER_TYPE = "bearer";

    private final String secret;
    private final long accessTokenExpireTime;
    private final long refreshTokenExpireTime;
    private Key key;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.access-token-expire-time}")long accessTokenExpireTime,
                            @Value("${jwt.refresh-token-expire-time}")long refreshTokenExpireTime
                            ) {
        this.secret = secret;
        this.accessTokenExpireTime = accessTokenExpireTime * 1000;
        this.refreshTokenExpireTime = refreshTokenExpireTime * 1000;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // ?????? ??????
    public TokenDto createToken(Authentication authentication) {
        // ????????? ????????????
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + this.accessTokenExpireTime);

        String accessToken = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        Date refreshTokenExpiresIn = new Date(now + this.refreshTokenExpireTime);
        String refreshToken = Jwts.builder()
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return TokenDto.builder()
                .grantType(BEARER_TYPE)
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpiresIn.getTime())
                .refreshToken(refreshToken)
                .username(authentication.getName())
                .build();

    }

    // ????????? ?????? Claim??? ?????????.
    // claim?????? ????????? ?????? ??????????????? ?????? ??????????????? authentication ?????? ??????
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if(claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("?????? ????????? ?????? ???????????????.");
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());


        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // ?????? ????????? ??????
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch(io.jsonwebtoken.security.SecurityException  | MalformedJwtException e) {
            logger.info("????????? JWT ???????????????.");
        } catch (ExpiredJwtException e) {
            logger.info("????????? JWT ???????????????.");
        } catch (UnsupportedJwtException e) {
            logger.info("???????????? ?????? JWT ???????????????.");
        } catch (IllegalArgumentException e) {
            logger.info("JWT ????????? ?????????????????????.");
        }
        return false;
    }

    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
