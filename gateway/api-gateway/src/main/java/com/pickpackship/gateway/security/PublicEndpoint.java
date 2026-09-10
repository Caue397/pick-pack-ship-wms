package com.pickpackship.gateway.security;

import org.springframework.http.HttpMethod;

record PublicEndpoint(HttpMethod method, String path) {
}
