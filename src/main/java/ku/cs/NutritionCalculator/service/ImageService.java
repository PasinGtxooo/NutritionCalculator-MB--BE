package ku.cs.NutritionCalculator.service;

import java.io.IOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.bucket}")
    private String bucket;

    private final RestTemplate restTemplate = new RestTemplate();

    public String saveImage(MultipartFile image) throws IOException {

        String originalFilename = image.getOriginalFilename();
        // แทนที่ช่องว่างและอักขระพิเศษด้วย underscore เพื่อป้องกัน IllegalArgumentException
        if (originalFilename != null) {
            originalFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
        String fileName = System.currentTimeMillis() + "_" + originalFilename;
        String uploadUrl = supabaseUrl +
                "/storage/v1/object/" +
                bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(image.getBytes(), headers);

        restTemplate.exchange(
                URI.create(uploadUrl),
                HttpMethod.POST,
                requestEntity,
                String.class);

        // URL ที่ใช้เรียกรูป
        return supabaseUrl +
                "/storage/v1/object/public/" +
                bucket + "/" + fileName;
    }
}
