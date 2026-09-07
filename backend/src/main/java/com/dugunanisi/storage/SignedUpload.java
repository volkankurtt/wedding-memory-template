package com.dugunanisi.storage;

public record SignedUpload(String signedUrl, String token, int expiresInSeconds) {
}
